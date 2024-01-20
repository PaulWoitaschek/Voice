package voice.playback.session

import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.data.Book
import voice.data.repo.BookRepository
import voice.logging.core.Logger
import voice.playback.player.VoicePlayer
import voice.playback.session.search.BookSearchHandler
import voice.playback.session.search.BookSearchParser
import javax.inject.Inject

class LibrarySessionCallback
@Inject constructor(
  private val mediaItemProvider: MediaItemProvider,
  private val scope: CoroutineScope,
  private val player: VoicePlayer,
  private val bookSearchParser: BookSearchParser,
  private val bookSearchHandler: BookSearchHandler,
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
  private val sleepTimerCommandUpdater: SleepTimerCommandUpdater,
  private val sleepTimer: SleepTimer,
  private val bookRepository: BookRepository,
) : MediaLibrarySession.Callback {

  override fun onAddMediaItems(
    mediaSession: MediaSession,
    controller: ControllerInfo,
    mediaItems: MutableList<MediaItem>,
  ): ListenableFuture<List<MediaItem>> {
    Logger.d("onAddMediaItems")
    return scope.future {
      mediaItems.map { item ->
        mediaItemProvider.item(item.mediaId) ?: item
      }
    }
  }

  override fun onSetMediaItems(
    mediaSession: MediaSession,
    controller: ControllerInfo,
    mediaItems: MutableList<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
  ): ListenableFuture<MediaItemsWithStartPosition> {
    Logger.d("onSetMediaItems(mediaItems.size=${mediaItems.size}, startIndex=$startIndex, startPosition=$startPositionMs)")
    val item = mediaItems.singleOrNull()
    return if (startIndex == C.INDEX_UNSET && startPositionMs == C.TIME_UNSET && item != null) {
      scope.future {
        onSetMediaItemsForSingleItem(item)
          ?: super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs).await()
      }
    } else {
      super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
    }
  }

  private suspend fun onSetMediaItemsForSingleItem(item: MediaItem): MediaItemsWithStartPosition? {
    val searchQuery = item.requestMetadata.searchQuery
    return if (searchQuery != null) {
      val search = bookSearchParser.parse(searchQuery, item.requestMetadata.extras)
      val searchResult = bookSearchHandler.handle(search) ?: return null
      currentBookId.updateData { searchResult.id }
      mediaItemProvider.mediaItemsWithStartPosition(searchResult)
    } else {
      (item.mediaId.toMediaIdOrNull() as? MediaId.Book)?.let { bookId ->
        currentBookId.updateData { bookId.id }
      }
      mediaItemProvider.mediaItemsWithStartPosition(item.mediaId)
    }
  }

  override fun onGetLibraryRoot(
    session: MediaLibrarySession,
    browser: ControllerInfo,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    val mediaItem = if (params?.isRecent == true) {
      mediaItemProvider.recent() ?: mediaItemProvider.root()
    } else {
      mediaItemProvider.root()
    }
    Logger.d("onGetLibraryRoot(isRecent=${params?.isRecent == true}). Returning ${mediaItem.mediaId}")
    return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, params))
  }

  override fun onGetItem(
    session: MediaLibrarySession,
    browser: ControllerInfo,
    mediaId: String,
  ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
    Logger.d("onGetItem(mediaId=$mediaId)")
    val item = mediaItemProvider.item(mediaId)
    if (item != null) {
      LibraryResult.ofItem(item, null)
    } else {
      LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
    }
  }

  override fun onGetChildren(
    session: MediaLibrarySession,
    browser: ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
    Logger.d("onGetChildren for $parentId")
    val children = mediaItemProvider.children(parentId)
    if (children != null) {
      LibraryResult.ofItemList(children, params)
    } else {
      LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
    }
  }

  override fun onPlaybackResumption(
    mediaSession: MediaSession,
    controller: ControllerInfo,
  ): ListenableFuture<MediaItemsWithStartPosition> {
    Logger.d("onPlaybackResumption")
    return scope.future {
      val currentBook = currentBook()
      if (currentBook != null) {
        mediaItemProvider.mediaItemsWithStartPosition(currentBook)
      } else {
        throw UnsupportedOperationException()
      }
    }
  }

  private suspend fun currentBook(): Book? {
    val bookId = currentBookId.data.first() ?: return null
    return bookRepository.get(bookId)
  }

  override fun onConnect(
    session: MediaSession,
    controller: ControllerInfo,
  ): ConnectionResult {
    Logger.d("onConnect to ${controller.packageName}")

    if (player.playbackState == Player.STATE_IDLE &&
      controller.packageName == "com.google.android.projection.gearhead"
    ) {
      Logger.d("onConnect to ${controller.packageName} and player is idle.")
      Logger.d("Preparing current book so it shows up as recently played")
      scope.launch {
        prepareCurrentBook()
      }
    }

    val connectionResult = super.onConnect(session, controller)
    val sessionCommands = connectionResult.availableSessionCommands
      .buildUpon()
      .add(SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, Bundle.EMPTY))
      .also {
        it.add(PublishedCustomCommand.Sleep.sessionCommand)
      }
      .build()
    return ConnectionResult.accept(
      sessionCommands,
      connectionResult.availablePlayerCommands,
    )
  }

  private suspend fun prepareCurrentBook() {
    val bookId = currentBookId.data.first() ?: return
    val book = bookRepository.get(bookId) ?: return
    val item = mediaItemProvider.mediaItem(book)
    player.setMediaItem(item)
    player.prepare()
  }

  override fun onCustomCommand(
    session: MediaSession,
    controller: ControllerInfo,
    customCommand: SessionCommand,
    args: Bundle,
  ): ListenableFuture<SessionResult> {
    when (customCommand) {
      PublishedCustomCommand.Sleep.sessionCommand -> {
        sleepTimer.setActive(!sleepTimer.sleepTimerActive())
      }
      else -> {
        val command = CustomCommand.parse(customCommand, args)
          ?: return super.onCustomCommand(session, controller, customCommand, args)
        when (command) {
          CustomCommand.ForceSeekToNext -> {
            player.forceSeekToNext()
          }

          CustomCommand.ForceSeekToPrevious -> {
            player.forceSeekToPrevious()
          }

          is CustomCommand.SetSkipSilence -> {
            player.setSkipSilenceEnabled(command.skipSilence)
          }
          is CustomCommand.SetGain -> {
            player.setGain(command.gain)
          }
        }
      }
    }

    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
  }

  override fun onPostConnect(
    session: MediaSession,
    controller: ControllerInfo,
  ) {
    super.onPostConnect(session, controller)
    sleepTimerCommandUpdater.update(session, controller, sleepTimer.sleepTimerActive())
  }
}

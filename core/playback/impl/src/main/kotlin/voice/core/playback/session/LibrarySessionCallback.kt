package voice.core.playback.session

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.zacsweers.metro.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import voice.core.data.Book
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.LockScreenSliderStore
import voice.core.playback.history.CommandSourceResolver
import voice.core.playback.player.VoicePlayer
import voice.core.playback.session.search.BookSearchHandler
import voice.core.playback.session.search.BookSearchParser
import voice.core.search.BookSearch

@Inject
class LibrarySessionCallback(
  private val mediaItemProvider: MediaItemProvider,
  private val scope: CoroutineScope,
  private val player: VoicePlayer,
  private val bookSearchParser: BookSearchParser,
  private val bookSearchHandler: BookSearchHandler,
  private val currentBookStoreId: CurrentBookStore,
  private val bookRepository: BookRepository,
  private val commandSourceResolver: CommandSourceResolver,
  private val context: Context,
  private val lockScreenSliderStore: LockScreenSliderStore,
  private val bookSearch: BookSearch,
) : MediaLibrarySession.Callback {

  private var notificationCommandsJob: Job? = null

  override fun onMediaButtonEvent(
    session: MediaSession,
    controllerInfo: ControllerInfo,
    intent: Intent,
  ): Boolean {
    commandSourceResolver.onMediaButtonEvent(controllerInfo)
    return super.onMediaButtonEvent(session, controllerInfo, intent)
  }

  override fun onAddMediaItems(
    mediaSession: MediaSession,
    controller: ControllerInfo,
    mediaItems: MutableList<MediaItem>,
  ): ListenableFuture<List<MediaItem>> {
    return scope.reportingFuture {
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
    Logger.d(
      "onSetMediaItems(mediaItems.size=${mediaItems.size}, startIndex=$startIndex, startPosition=$startPositionMs)",
    )
    val item = mediaItems.singleOrNull()
    if (startIndex == C.INDEX_UNSET && startPositionMs == C.TIME_UNSET && item != null) {
      return scope.reportingFuture {
        onSetMediaItemsForSingleItem(item)
          ?: super.onSetMediaItems(
              mediaSession,
              controller,
              mediaItems,
              startIndex,
              startPositionMs,
            )
            .await()
      }
    }
    // The app sends all chapter marks of the book. VoicePlayer loads the book once, so resolving
    // every chapter mark would only slow it down.
    if (mediaItems.firstOrNull()?.mediaId?.toMediaIdOrNull() is MediaId.ChapterMark) {
      return Futures.immediateFuture(
        MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs),
      )
    }
    return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
  }

  private suspend fun onSetMediaItemsForSingleItem(item: MediaItem): MediaItemsWithStartPosition? {
    val searchQuery = item.requestMetadata.searchQuery
    return if (searchQuery != null) {
      val search = bookSearchParser.parse(searchQuery, item.requestMetadata.extras)
      val searchResult = bookSearchHandler.handle(search) ?: return null
      currentBookStoreId.updateData { searchResult.id }
      mediaItemProvider.mediaItemsWithStartPosition(searchResult)
    } else {
      item.mediaId.toMediaIdOrNull()?.bookId?.let { bookId ->
        currentBookStoreId.updateData { bookId }
      }
      mediaItemProvider.mediaItemsWithStartPosition(item.mediaId)
    }
  }

  override fun onGetLibraryRoot(
    session: MediaLibrarySession,
    browser: ControllerInfo,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    Logger.d("onGetLibraryRoot(isRecent=${params?.isRecent == true})")
    val mediaItem =
      if (params?.isRecent == true) {
        mediaItemProvider.recent()
          ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED))
      } else {
        mediaItemProvider.root()
      }
    return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, params))
  }

  override fun onGetItem(
    session: MediaLibrarySession,
    browser: ControllerInfo,
    mediaId: String,
  ): ListenableFuture<LibraryResult<MediaItem>> = scope.reportingFuture {
    val item = mediaItemProvider.item(mediaId)
    if (item != null) {
      LibraryResult.ofItem(item, null)
    } else {
      LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }
  }

  override fun onGetChildren(
    session: MediaLibrarySession,
    browser: ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.reportingFuture {
    val children = mediaItemProvider.children(parentId)
    if (children != null) {
      LibraryResult.ofItemList(children, params)
    } else {
      LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }
  }

  override fun onSearch(
    session: MediaLibrarySession,
    browser: ControllerInfo,
    query: String,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<Void>> = scope.reportingFuture {
    session.notifySearchResultChanged(browser, query, search(query).size, params)
    LibraryResult.ofVoid()
  }

  override fun onGetSearchResult(
    session: MediaLibrarySession,
    browser: ControllerInfo,
    query: String,
    page: Int,
    pageSize: Int,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.reportingFuture {
    LibraryResult.ofItemList(search(query), params)
  }

  private suspend fun search(query: String): List<MediaItem> {
    return bookSearch.search(query).map { mediaItemProvider.mediaItem(it.book) }
  }

  override fun onPlaybackResumption(
    mediaSession: MediaSession,
    controller: ControllerInfo,
    isForPlayback: Boolean,
  ): ListenableFuture<MediaItemsWithStartPosition> {
    Logger.d("onPlaybackResumption")
    return scope.reportingFuture {
      val currentBook = currentBook() ?: throw UnsupportedOperationException()
      if (isForPlayback) {
        mediaItemProvider.mediaItemsWithStartPosition(currentBook)
      } else {
        // System UI shows this item on its resumption card, so it needs the book, not a chapter.
        MediaItemsWithStartPosition(
          listOf(mediaItemProvider.mediaItem(currentBook)),
          C.INDEX_UNSET,
          C.TIME_UNSET,
        )
      }
    }
  }

  private suspend fun currentBook(): Book? {
    val bookId = currentBookStoreId.data.first() ?: return null
    return bookRepository.get(bookId)
  }

  override fun onConnect(
    session: MediaSession,
    controller: ControllerInfo,
  ): ConnectionResult {
    if (controller.packageName != context.packageName) {
      val controllerKind =
        when {
          session.isAutoCompanionController(controller) -> "Android Auto"
          session.isAutomotiveController(controller) -> "Android Automotive"
          else -> "another app"
        }
      Logger.d("onConnect from $controllerKind")
    }

    if (
      player.playbackState == Player.STATE_IDLE &&
        controller.packageName == "com.google.android.projection.gearhead"
    ) {
      Logger.d("Preparing current book so it shows up as recently played")
      scope.launch {
        prepareCurrentBook()
      }
    }

    // The service is exported, but the custom commands are only meant for the app's own
    // PlayerController.
    val sessionCommands =
      if (controller.packageName == context.packageName) {
        sessionCommandsWithCustomCommand()
      } else {
        ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
      }
    return ConnectionResult.accept(sessionCommands, ConnectionResult.DEFAULT_PLAYER_COMMANDS)
  }

  private fun sessionCommandsWithCustomCommand(): SessionCommands {
    return ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
      .add(SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, Bundle.EMPTY))
      .build()
  }

  override fun onPostConnect(
    session: MediaSession,
    controller: ControllerInfo,
  ) {
    if (!session.isMediaNotificationController(controller)) return
    // The notification controller's commands become the platform session's actions, which the lock
    // screen reads.
    val sessionCommands = sessionCommandsWithCustomCommand()
    notificationCommandsJob?.cancel()
    notificationCommandsJob = scope.launch {
      lockScreenSliderStore.data.distinctUntilChanged().collect { lockScreenSlider ->
        val playerCommands =
          if (lockScreenSlider) {
            ConnectionResult.DEFAULT_PLAYER_COMMANDS
          } else {
            ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
              .remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
              .build()
          }
        session.setAvailableCommands(controller, sessionCommands, playerCommands)
      }
    }
  }

  override fun onDisconnected(
    session: MediaSession,
    controller: ControllerInfo,
  ) {
    if (session.isMediaNotificationController(controller)) {
      notificationCommandsJob?.cancel()
      notificationCommandsJob = null
    }
  }

  private suspend fun prepareCurrentBook() {
    val bookId = currentBookStoreId.data.first() ?: return
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
    val command =
      CustomCommand.parse(customCommand, args)
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

    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
  }
}

// Media3 only logs a failed future. An UnsupportedOperationException is how it expects to be told
// that something isn't supported, so that one isn't a bug.
private fun <T> CoroutineScope.reportingFuture(
  block: suspend CoroutineScope.() -> T,
): ListenableFuture<T> = future {
  try {
    block()
  } catch (e: Exception) {
    if (e !is CancellationException && e !is UnsupportedOperationException) {
      Logger.nonFatal(e)
    }
    throw e
  }
}

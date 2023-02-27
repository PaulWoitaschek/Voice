package voice.playback.session

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.guava.future
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
) : MediaLibraryService.MediaLibrarySession.Callback {

  override fun onAddMediaItems(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo,
    mediaItems: MutableList<MediaItem>,
  ): ListenableFuture<List<MediaItem>> = scope.future {
    mediaItems.map { item ->
      mediaItemProvider.item(item.mediaId) ?: item
    }
  }

  override fun onSetMediaItems(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo,
    mediaItems: MutableList<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
  ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
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

  private suspend fun onSetMediaItemsForSingleItem(item: MediaItem): MediaSession.MediaItemsWithStartPosition? {
    val searchQuery = item.requestMetadata.searchQuery
    return if (searchQuery != null) {
      val search = bookSearchParser.parse(searchQuery, item.requestMetadata.extras)
      val searchResult = bookSearchHandler.handle(search) ?: return null
      mediaItemProvider.mediaItemsWithStartPosition(searchResult)
    } else {
      mediaItemProvider.mediaItemsWithStartPosition(item.mediaId)
    }
  }

  override fun onGetLibraryRoot(
    session: MediaLibraryService.MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    params: MediaLibraryService.LibraryParams?,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    return Futures.immediateFuture(LibraryResult.ofItem(mediaItemProvider.root(), params))
  }

  override fun onGetItem(
    session: MediaLibraryService.MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    mediaId: String,
  ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
    val item = mediaItemProvider.item(mediaId)
    if (item != null) {
      LibraryResult.ofItem(item, null)
    } else {
      LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
    }
  }

  override fun onGetChildren(
    session: MediaLibraryService.MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: MediaLibraryService.LibraryParams?,
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
    val children = mediaItemProvider.children(parentId)
    if (children != null) {
      LibraryResult.ofItemList(children, params)
    } else {
      LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
    }
  }

  override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
    val connectionResult = super.onConnect(session, controller)
    val sessionCommands = connectionResult.availableSessionCommands
      .buildUpon()
      .add(SessionCommand(CustomCommand.CustomCommandAction, Bundle.EMPTY))
      .also {
        it.add(PublishedCustomCommand.SeekBackwards.sessionCommand)
        it.add(PublishedCustomCommand.SeekForward.sessionCommand)
      }
      .build()
    return MediaSession.ConnectionResult.accept(
      sessionCommands,
      connectionResult.availablePlayerCommands,
    )
  }

  override fun onCustomCommand(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
    customCommand: SessionCommand,
    args: Bundle,
  ): ListenableFuture<SessionResult> {
    when (customCommand) {
      PublishedCustomCommand.SeekForward.sessionCommand -> {
        player.seekForward()
      }
      PublishedCustomCommand.SeekBackwards.sessionCommand -> {
        player.seekBack()
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
        }
      }
    }
    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
  }
}

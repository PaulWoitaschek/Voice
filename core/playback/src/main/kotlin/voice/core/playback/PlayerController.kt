package voice.core.playback

import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.logging.core.Logger
import voice.core.playback.misc.Decibel
import voice.core.playback.session.CustomCommand
import voice.core.playback.session.MediaId
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.PlaybackService
import voice.core.playback.session.sendCustomCommand
import voice.core.playback.session.toMediaIdOrNull
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

@Inject
class PlayerController(
  private val context: Context,
  @CurrentBookStore
  private val currentBookStoreId: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val mediaItemProvider: MediaItemProvider,
) {

  private var _controller: Deferred<MediaController> = newControllerAsync()

  private fun newControllerAsync() = MediaController
    .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
    .buildAsync()
    .asDeferred()

  private val controller: Deferred<MediaController>
    get() {
      if (_controller.isCompleted) {
        val completedController = _controller.getCompleted()
        if (!completedController.isConnected) {
          completedController.release()
          _controller = newControllerAsync()
        }
      }
      return _controller
    }
  private val scope = CoroutineScope(Dispatchers.Main.immediate)

  fun setPosition(
    time: Long,
    id: ChapterId,
  ) = executeAfterPrepare { controller ->
    val bookId = currentBookStoreId.data.first() ?: return@executeAfterPrepare
    val book = bookRepository.get(bookId) ?: return@executeAfterPrepare
    val index = book.chapters.indexOfFirst { it.id == id }
    if (index != -1) {
      controller.seekTo(index, time)
    }
  }

  fun pauseIfCurrentBookDifferentFrom(id: BookId) {
    scope.launch {
      val controller = awaitConnect() ?: return@launch
      val currentBookId = controller.currentBookId()
      if (currentBookId != null && currentBookId != id) {
        controller.pause()
      }
    }
  }

  fun skipSilence(skip: Boolean) = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.SetSkipSilence(skip))
  }

  fun fastForward() = executeAfterPrepare { controller ->
    controller.seekForward()
  }

  fun rewind() = executeAfterPrepare { controller ->
    controller.seekBack()
  }

  fun previous() = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.ForceSeekToPrevious)
  }

  fun next() = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.ForceSeekToNext)
  }

  fun play() = executeAfterPrepare { controller ->
    controller.play()
  }

  fun playPause() = executeAfterPrepare { controller ->
    if (controller.isPlaying) {
      controller.pause()
    } else {
      controller.play()
    }
  }

  private suspend fun maybePrepare(controller: MediaController): Boolean {
    val bookId = currentBookStoreId.data.first() ?: return false
    if (controller.currentBookId() == bookId &&
      controller.playbackState in listOf(Player.STATE_READY, Player.STATE_BUFFERING)
    ) {
      return true
    }
    val book = bookRepository.get(bookId) ?: return false
    controller.setMediaItem(mediaItemProvider.mediaItem(book))
    controller.prepare()
    return true
  }

  private fun MediaController.currentBookId(): BookId? {
    val currentMediaItem = currentMediaItem ?: return null
    val mediaId = currentMediaItem.mediaId.toMediaIdOrNull() ?: return null
    return when (mediaId) {
      is MediaId.Book -> mediaId.id
      is MediaId.Chapter -> mediaId.bookId
      MediaId.Recent -> null
      MediaId.Root -> null
    }
  }

  fun pauseWithRewind(rewind: Duration) = executeAfterPrepare { controller ->
    controller.pause()
    controller.seekTo((controller.currentPosition - rewind.inWholeMilliseconds).coerceAtLeast(0))
  }

  fun setSpeed(speed: Float) = executeAfterPrepare { controller ->
    controller.setPlaybackSpeed(speed)
  }

  fun setGain(gain: Decibel) = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.SetGain(gain))
  }

  fun setVolume(volume: Float) = executeAfterPrepare {
    require(volume in 0F..1F)
    it.volume = volume
  }

  private inline fun executeAfterPrepare(crossinline action: suspend (MediaController) -> Unit) {
    scope.launch {
      val controller = awaitConnect() ?: return@launch
      if (maybePrepare(controller)) {
        action(controller)
      }
    }
  }

  suspend fun awaitConnect(): MediaController? {
    return try {
      controller.await()
    } catch (e: Exception) {
      if (e is CancellationException) coroutineContext.ensureActive()
      Logger.w(e, "Error while connecting to media controller")
      null
    }
  }
}

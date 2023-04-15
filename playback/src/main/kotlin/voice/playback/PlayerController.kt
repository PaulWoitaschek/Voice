package voice.playback

import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.data.BookContent
import voice.data.ChapterId
import voice.data.repo.BookRepository
import voice.playback.misc.Decibel
import voice.playback.misc.VolumeGain
import voice.playback.session.CustomCommand
import voice.playback.session.MediaId
import voice.playback.session.MediaItemProvider
import voice.playback.session.PlaybackService
import voice.playback.session.sendCustomCommand
import voice.playback.session.toMediaIdOrNull
import javax.inject.Inject
import kotlin.time.Duration

class PlayerController
@Inject constructor(
  private val context: Context,
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val volumeGain: VolumeGain,
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

  fun setPosition(time: Long, id: ChapterId) = executeAfterPrepare { controller ->
    val bookId = currentBookId.data.first() ?: return@executeAfterPrepare
    val book = bookRepository.get(bookId) ?: return@executeAfterPrepare
    val index = book.chapters.indexOfFirst { it.id == id }
    if (index != -1) {
      controller.seekTo(index, time)
    }
  }

  fun skipSilence(skip: Boolean) = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.SetSkipSilence(skip))
    updateBook { it.copy(skipSilence = skip) }
  }

  private suspend fun updateBook(update: (BookContent) -> BookContent) {
    val bookId = currentBookId.data.first() ?: return
    bookRepository.updateBook(bookId, update)
  }

  fun fastForward() = executeAfterPrepare { controller ->
    controller.seekForward()
  }

  fun rewind() = executeAfterPrepare { controller ->
    controller.seekBack()
  }

  fun previous() = executeAfterPrepare {
    it.sendCustomCommand(CustomCommand.ForceSeekToPrevious)
  }

  fun next() = executeAfterPrepare {
    it.sendCustomCommand(CustomCommand.ForceSeekToNext)
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

  suspend fun maybePrepare() {
    maybePrepare(awaitConnect())
  }

  private suspend fun maybePrepare(controller: MediaController): Boolean {
    val bookId = currentBookId.data.first() ?: return false
    if (controller.currentBookId() == bookId &&
      controller.playbackState in listOf(Player.STATE_READY, Player.STATE_BUFFERING)
    ) {
      return true
    }
    val book = bookRepository.get(bookId) ?: return false
    val mediaItems = book.chapters.map { mediaItemProvider.mediaItem(it, book.content) }
    controller.setMediaItems(
      mediaItems,
      book.content.currentChapterIndex,
      book.content.positionInChapter,
    )
    controller.sendCustomCommand(CustomCommand.SetSkipSilence(book.content.skipSilence))
    controller.setPlaybackSpeed(book.content.playbackSpeed)
    controller.prepare()
    volumeGain.gain = Decibel(book.content.gain)
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

  fun pauseWithRewind(rewind: Duration) = executeAfterPrepare {
    it.pause()
    it.seekTo((it.currentPosition - rewind.inWholeMilliseconds.coerceAtLeast(0)))
  }

  fun setSpeed(speed: Float) = executeAfterPrepare { player ->
    player.setPlaybackSpeed(speed)
    updateBook { it.copy(playbackSpeed = speed) }
  }

  fun setGain(gain: Decibel) {
    volumeGain.gain = gain
    scope.launch {
      updateBook { it.copy(gain = gain.value) }
    }
  }

  fun setVolume(volume: Float) = executeAfterPrepare {
    require(volume in 0F..1F)
    it.volume = volume
  }

  private inline fun executeAfterPrepare(crossinline action: suspend (MediaController) -> Unit) {
    scope.launch {
      val controller = controller.await()
      if (maybePrepare(controller)) {
        action(controller)
      }
    }
  }

  suspend fun awaitConnect(): MediaController = controller.await()
}

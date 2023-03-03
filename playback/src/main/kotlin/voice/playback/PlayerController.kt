package voice.playback

import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
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
import voice.playback.session.PlaybackService
import voice.playback.session.sendCustomCommand
import voice.playback.session.toMediaIdOrNull
import voice.playback.session.toMediaItem
import javax.inject.Inject
import kotlin.time.Duration

class PlayerController
@Inject constructor(
  context: Context,
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val volumeGain: VolumeGain,
) {

  private val controller: Deferred<MediaController> = MediaController
    .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
    .buildAsync()
    .asDeferred()

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
    if ((controller.currentMediaItem?.mediaId?.toMediaIdOrNull() as MediaId.Chapter?)?.bookId == bookId) {
      return true
    }
    val book = bookRepository.get(bookId) ?: return false
    val mediaItems = book.chapters.map { it.toMediaItem(book.content) }
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

  fun pauseWithRewind(rewind: Duration) = executeAfterPrepare {
    it.pause()
    it.seekTo((it.currentPosition - rewind.inWholeMilliseconds.coerceAtLeast(0)))
  }

  fun setSpeed(speed: Float) = executeAfterPrepare {
    it.setPlaybackSpeed(speed)
  }

  fun setGain(gain: Decibel) {
    scope.launch {
      updateBook { it.copy(gain = gain.value) }
      volumeGain.gain = gain
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

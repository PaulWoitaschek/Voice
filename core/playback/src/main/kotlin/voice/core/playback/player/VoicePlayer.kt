package voice.core.playback.player

import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ChapterRepo
import voice.core.data.store.AutoRewindAmountStore
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.SeekTimeStore
import voice.core.logging.core.Logger
import voice.core.playback.misc.Decibel
import voice.core.playback.misc.VolumeGain
import voice.core.playback.session.MediaId
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.toMediaIdOrNull
import voice.core.sleeptimer.SleepTimer
import java.time.Instant
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
class VoicePlayer(
  private val player: Player,
  private val repo: BookRepository,
  @CurrentBookStore
  private val currentBookStoreId: DataStore<BookId?>,
  @SeekTimeStore
  private val seekTimeStore: DataStore<Int>,
  @AutoRewindAmountStore
  private val autoRewindAmountStore: DataStore<Int>,
  private val mediaItemProvider: MediaItemProvider,
  private val scope: CoroutineScope,
  private val chapterRepo: ChapterRepo,
  private val volumeGain: VolumeGain,
  private val sleepTimer: SleepTimer,
) : ForwardingPlayer(player) {

  fun forceSeekToNext() {
    scope.launch {
      val currentMediaItem = player.currentMediaItem ?: return@launch
      val marks = currentMediaItem.chapter()?.chapterMarks ?: return@launch
      val currentMarkIndex = marks.indexOfFirst { mark ->
        player.currentPosition in mark.startMs..mark.endMs
      }
      val nextMark = marks.getOrNull(currentMarkIndex + 1)
      if (nextMark != null) {
        player.seekTo(nextMark.startMs)
      } else {
        player.seekToNext()
      }
    }
  }

  private suspend fun MediaItem.chapter(): Chapter? {
    val mediaId = mediaId.toMediaIdOrNull() ?: return null
    if (mediaId !is MediaId.Chapter) return null
    return chapterRepo.get(mediaId.chapterId)
  }

  fun forceSeekToPrevious() {
    scope.launch {
      val currentMediaItem = player.currentMediaItem ?: return@launch
      val marks = currentMediaItem.chapter()?.chapterMarks ?: return@launch
      val currentPosition = player.currentPosition
      val currentMark = marks.firstOrNull { mark ->
        currentPosition in mark.startMs..mark.endMs
      } ?: marks.last()

      if (currentPosition - currentMark.startMs > THRESHOLD_FOR_BACK_SEEK_MS) {
        player.seekTo(currentMark.startMs)
      } else {
        val currentMarkIndex = marks.indexOf(currentMark)
        val previousMark = marks.getOrNull(currentMarkIndex - 1)
        if (previousMark != null) {
          player.seekTo(previousMark.startMs)
        } else {
          val currentMediaItemIndex = player.currentMediaItemIndex
          if (currentMediaItemIndex > 0) {
            val previousMediaItemIndex = currentMediaItemIndex - 1
            val previousMediaItemMarks = player.getMediaItemAt(previousMediaItemIndex).chapter()?.chapterMarks
              ?: return@launch
            player.seekTo(previousMediaItemIndex, previousMediaItemMarks.last().startMs)
          } else {
            player.seekTo(0)
          }
        }
      }
    }
  }

  override fun getAvailableCommands(): Player.Commands {
    /**
     * On Android 13, the notification always shows the "skip to next" and "skip to previous"
     * actions.
     * However these are also used internally when seeking for example through a bluetooth headset
     * We use these and delegate them to fast forward / rewind.
     * The player however only advertises the seek to next and previous item in the case
     * that it's not the first or last track. Therefore we manually advertise that these
     * are available.
     */
    return super.getAvailableCommands()
      .buildUpon()
      .addAll(
        COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        COMMAND_SEEK_TO_PREVIOUS,
        COMMAND_SEEK_TO_NEXT,
        COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
      )
      .build()
  }

  override fun seekToPreviousMediaItem() {
    seekBack()
  }

  override fun seekToNextMediaItem() {
    seekForward()
  }

  override fun seekToPrevious() {
    seekBack()
  }

  override fun seekToNext() {
    seekForward()
  }

  override fun seekBack() {
    scope.launch {
      val skipAmount = seekTimeStore.data.first().seconds

      val currentPosition = player.currentPosition.takeUnless { it == C.TIME_UNSET }
        ?.milliseconds
        ?.coerceAtLeast(ZERO)
        ?: return@launch

      val newPosition = currentPosition - skipAmount
      if (newPosition < ZERO) {
        val previousMediaItemIndex = previousMediaItemIndex.takeUnless { it == C.INDEX_UNSET }
        if (previousMediaItemIndex == null) {
          player.seekTo(0)
        } else {
          val previousMediaItem = player.getMediaItemAt(previousMediaItemIndex)
          val chapter = previousMediaItem.chapter() ?: return@launch
          val previousMediaItemDuration = chapter.duration.milliseconds
          player.seekTo(previousMediaItemIndex, (previousMediaItemDuration - newPosition.absoluteValue).inWholeMilliseconds)
        }
      } else {
        player.seekTo(newPosition.inWholeMilliseconds)
      }
    }
  }

  override fun seekForward() {
    scope.launch {
      val skipAmount = seekTimeStore.data.first().seconds

      val currentPosition = player.currentPosition.takeUnless { it == C.TIME_UNSET }
        ?.milliseconds
        ?.coerceAtLeast(ZERO)
        ?: return@launch
      val newPosition = currentPosition + skipAmount

      val duration = player.duration.takeUnless { it == C.TIME_UNSET }
        ?.milliseconds
        ?: return@launch

      if (newPosition > duration) {
        val nextMediaItemIndex = nextMediaItemIndex.takeUnless { it == C.INDEX_UNSET }
          ?: return@launch
        player.seekTo(nextMediaItemIndex, (duration - newPosition).absoluteValue.inWholeMilliseconds)
      } else {
        player.seekTo(newPosition.inWholeMilliseconds)
      }
    }
  }

  override fun play() {
    playWhenReady = true
  }

  override fun setPlayWhenReady(playWhenReady: Boolean) {
    Logger.d("setPlayWhenReady=$playWhenReady")
    if (playWhenReady) {
      updateLastPlayedAt()
    } else {
      val currentPosition = player.currentPosition.takeUnless { it == C.TIME_UNSET }?.milliseconds ?: ZERO
      if (currentPosition > ZERO) {
        val autoRewindAmount = runBlocking { autoRewindAmountStore.data.first().seconds }
        seekTo(
          (currentPosition - autoRewindAmount)
            .coerceAtLeast(ZERO)
            .inWholeMilliseconds,
        )
      }
    }
    super.setPlayWhenReady(playWhenReady)
  }

  override fun pause() {
    playWhenReady = false
  }

  private fun updateLastPlayedAt() {
    scope.launch {
      currentBookStoreId.data.first()?.let { bookId ->
        repo.updateBook(bookId) {
          val lastPlayedAt = Instant.now()
          Logger.v("Update ${it.name}: lastPlayedAt to $lastPlayedAt")
          it.copy(lastPlayedAt = lastPlayedAt)
        }
      }
    }
  }

  override fun getPlaybackState(): Int = when (val state = super.getPlaybackState()) {
    // redirect buffering to ready to prevent visual artifacts on seeking
    STATE_BUFFERING -> STATE_READY
    else -> state
  }

  override fun setMediaItem(
    mediaItem: MediaItem,
    startPositionMs: Long,
  ) {
    setBook(mediaItem)
  }

  override fun setMediaItem(
    mediaItem: MediaItem,
    resetPosition: Boolean,
  ) {
    setBook(mediaItem)
  }

  override fun setMediaItems(mediaItems: List<MediaItem>) {
    val first = mediaItems.firstOrNull() ?: return
    setBook(first)
  }

  override fun setMediaItems(
    mediaItems: List<MediaItem>,
    resetPosition: Boolean,
  ) {
    val first = mediaItems.firstOrNull() ?: return
    setBook(first)
  }

  override fun setMediaItem(mediaItem: MediaItem) {
    setBook(mediaItem)
  }

  override fun setMediaItems(
    mediaItems: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
  ) {
    val first = mediaItems.firstOrNull() ?: return
    setBook(first)
  }

  private fun setBook(mediaItem: MediaItem) {
    Logger.v("setBook(${mediaItem.mediaId})")
    val mediaId = mediaItem.mediaId.toMediaIdOrNull()
    if (mediaId != null) {
      if (mediaId is MediaId.Book) {
        val book = runBlocking {
          repo.get(mediaId.id)
        }
        if (book != null) {
          player.setPlaybackSpeed(book.content.playbackSpeed)
          setSkipSilenceEnabled(book.content.skipSilence)
          volumeGain.gain = Decibel(book.content.gain)
          val chapters = mediaItemProvider.chapters(book)
          player.setMediaItems(
            chapters,
            book.content.currentChapterIndex,
            book.content.positionInChapter,
          )
          registerChapterMarkCallbacks(chapters)
        }
      } else {
        Logger.w("Unexpected mediaId=$mediaId")
      }
    }
  }

  private fun registerChapterMarkCallbacks(chapters: List<MediaItem>) {
    if (player is ExoPlayer) {
      assert(chapters.size == player.mediaItemCount)
      val boundaryHandler = PlayerMessage.Target { _, payload ->
        if (payload is Pair<*, *> && sleepTimer.sleepAtEoc) {
          player.pause()
          player.seekTo(payload.first as Int, payload.second as Long)
          sleepTimer.disable()
        }
      }
      chapters.forEachIndexed { index, mediaItem ->
        val mediaId = mediaItem.mediaId.toMediaIdOrNull() ?: return
        if (mediaId !is MediaId.Chapter) return
        val marks = runBlocking { (chapterRepo.get(mediaId.chapterId)?.chapterMarks?.map { mark -> mark.startMs } ?: listOf(0L)) }
        marks.forEach { startMs ->
          player.createMessage(boundaryHandler)
            .setPosition(index, startMs)
            .setPayload(Pair(index, startMs))
            .setDeleteAfterDelivery(false)
            .setLooper(Looper.getMainLooper())
            .send()
        }
      }
    }
  }

  override fun setPlaybackSpeed(speed: Float) {
    super.setPlaybackSpeed(speed)
    scope.launch {
      updateBook { it.copy(playbackSpeed = speed) }
    }
  }

  fun setSkipSilenceEnabled(enabled: Boolean): Boolean {
    scope.launch {
      updateBook { it.copy(skipSilence = enabled) }
    }
    return if (player is ExoPlayer) {
      player.skipSilenceEnabled = enabled
      true
    } else {
      false
    }
  }

  fun setGain(gain: Decibel) {
    volumeGain.gain = gain
    scope.launch {
      updateBook { it.copy(gain = gain.value) }
    }
  }

  private suspend fun updateBook(update: (BookContent) -> BookContent) {
    val bookId = currentBookStoreId.data.first() ?: return
    repo.updateBook(bookId, update)
  }
}

private const val THRESHOLD_FOR_BACK_SEEK_MS = 2000

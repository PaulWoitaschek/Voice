package voice.core.playback.player

import androidx.datastore.core.DataStore
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import voice.core.analytics.api.Analytics
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ChapterRepo
import voice.core.data.store.AutoRewindAmountStore
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.SeekTimeStore
import voice.core.logging.api.Logger
import voice.core.playback.misc.Decibel
import voice.core.playback.misc.VolumeGain
import voice.core.playback.session.MediaId
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.markDurationMs
import voice.core.playback.session.playbackItemForPosition
import voice.core.playback.session.positionInMediaItem
import voice.core.playback.session.toMediaIdOrNull
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerState
import java.time.Instant
import kotlin.time.Duration
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
  private val volumeGain: VolumeGain,
  private val sleepTimer: SleepTimer,
  private val analytics: Analytics,
) : ForwardingPlayer(player) {

  private val endOfChapterSleepTimerListener = object : Player.Listener {
    override fun onPositionDiscontinuity(
      oldPosition: Player.PositionInfo,
      newPosition: Player.PositionInfo,
      reason: Int,
    ) {
      if (reason == DISCONTINUITY_REASON_AUTO_TRANSITION) {
        pauseAndDisableSleepTimerIfEndOfChapter()
      }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
      if (playbackState == STATE_ENDED) {
        pauseAndDisableSleepTimerIfEndOfChapter()
      }
    }

    private fun pauseAndDisableSleepTimerIfEndOfChapter() {
      if (sleepTimer.state.value !is SleepTimerState.Enabled.WithEndOfChapter) return
      Logger.v("Pausing due to EndOfChapter")
      sleepTimer.disable()
      player.pause()
    }
  }

  init {
    player.addListener(endOfChapterSleepTimerListener)
  }

  fun forceSeekToNext() {
    scope.launch {
      val nextMediaItemIndex = player.nextMediaItemIndex.takeUnless { it == C.INDEX_UNSET }
        ?: return@launch
      player.seekTo(nextMediaItemIndex, 0)
    }
  }

  fun forceSeekToPrevious() {
    scope.launch {
      val currentPosition = player.currentPosition
      if (currentPosition > THRESHOLD_FOR_BACK_SEEK_MS) {
        player.seekTo(0)
      } else {
        val previousMediaItemIndex = player.previousMediaItemIndex.takeUnless { it == C.INDEX_UNSET }
        if (previousMediaItemIndex != null) {
          player.seekTo(previousMediaItemIndex, 0)
        } else {
          player.seekTo(0)
        }
      }
    }
  }

  override fun getAvailableCommands(): Player.Commands {
    // On Android 13, the notification always shows the "skip to next" and "skip to previous"
    // actions.
    // However these are also used internally when seeking for example through a bluetooth headset
    // We use these and delegate them to fast forward / rewind.
    // The player however only advertises the seek to next and previous item in the case
    // that it's not the first or last track. Therefore we manually advertise that these
    // are available.
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
      seekBackBy(seekTimeStore.data.first().seconds)
    }
  }

  private suspend fun seekBackBy(skipAmount: Duration) {
    seekBackBy(
      skipAmount = skipAmount,
      crossMediaItems = true,
    )
  }

  private suspend fun seekBackBy(
    skipAmount: Duration,
    crossMediaItems: Boolean,
  ) {
    var currentPosition = player.currentPosition.takeUnless { it == C.TIME_UNSET }
      ?.milliseconds
      ?.coerceAtLeast(ZERO)
      ?: return
    var remaining = skipAmount
    var mediaItemIndex = player.currentMediaItemIndex.takeUnless { it == C.INDEX_UNSET } ?: return

    while (remaining > currentPosition) {
      if (!crossMediaItems) {
        player.seekTo(mediaItemIndex, 0)
        return
      }
      remaining -= currentPosition
      val previousMediaItemIndex = mediaItemIndex - 1
      if (previousMediaItemIndex < 0) {
        player.seekTo(0)
        return
      }
      val previousMediaItem = player.getMediaItemAt(previousMediaItemIndex)
      currentPosition = previousMediaItem.durationMs()?.milliseconds ?: return
      mediaItemIndex = previousMediaItemIndex
    }

    player.seekTo(mediaItemIndex, (currentPosition - remaining).inWholeMilliseconds)
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
    analytics.event(if (playWhenReady) "play" else "pause")

    if (playWhenReady) {
      updateLastPlayedAt()
    } else {
      val currentPosition = player.currentPosition.takeUnless { it == C.TIME_UNSET }?.milliseconds ?: ZERO
      if (currentPosition > ZERO) {
        scope.launch {
          seekBackBy(
            skipAmount = autoRewindAmountStore.data.first().seconds,
            crossMediaItems = false,
          )
        }
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
          val currentPlaybackItem = book.playbackItemForPosition(
            chapterId = book.content.currentChapter,
            positionInChapterMs = book.content.positionInChapter,
          ) ?: return
          val mediaItems = mediaItemProvider.playbackItems(book)
          player.setMediaItems(
            mediaItems,
            currentPlaybackItem.index,
            currentPlaybackItem.positionInMediaItem(book.content.positionInChapter),
          )
        }
      } else {
        Logger.w("Unexpected mediaId=$mediaId")
      }
    }
  }

  override fun setPlaybackSpeed(speed: Float) {
    super.setPlaybackSpeed(speed)
    scope.launch {
      updateBook { it.copy(playbackSpeed = speed) }
    }
  }

  fun setSkipSilenceEnabled(enabled: Boolean) {
    scope.launch {
      updateBook { it.copy(skipSilence = enabled) }
    }
    if (player is ExoPlayer) {
      player.skipSilenceEnabled = enabled
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

private suspend fun MediaItem.durationMs(chapterRepo: ChapterRepo? = null): Long? {
  return when (val mediaId = mediaId.toMediaIdOrNull()) {
    is MediaId.ChapterMark -> mediaId.markDurationMs
    is MediaId.Chapter -> chapterRepo?.get(mediaId.chapterId)?.duration
    is MediaId.Book,
    MediaId.Recent,
    MediaId.Root,
    null,
    -> null
  }
}

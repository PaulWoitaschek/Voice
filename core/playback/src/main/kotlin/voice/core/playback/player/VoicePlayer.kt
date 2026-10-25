package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import voice.core.analytics.api.Analytics
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.AutoRewindAmountStore
import voice.core.playback.LastPlaybackSpeedStore
import voice.core.playback.SeekTimeStore
import voice.core.playback.di.PlaybackScope
import voice.core.playback.history.CommandSource
import voice.core.playback.history.CommandSourceResolver
import voice.core.playback.history.ListeningHistoryRecorder
import voice.core.playback.history.PlaybackPosition
import voice.core.playback.history.playbackPosition
import voice.core.playback.misc.Decibel
import voice.core.playback.misc.VolumeGain
import voice.core.playback.session.MediaId
import voice.core.playback.session.bookId
import voice.core.playback.session.playbackItemForPosition
import voice.core.playback.session.positionInMediaItem
import voice.core.playback.session.toMediaIdOrNull
import voice.core.playback.withDefaultSpeed
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerState
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(PlaybackScope::class)
class VoicePlayer(
  private val player: ChapterMarkPlayer,
  private val repo: BookRepository,
  private val currentBookStoreId: CurrentBookStore,
  private val seekTimeStore: SeekTimeStore,
  private val autoRewindAmountStore: AutoRewindAmountStore,
  private val lastPlaybackSpeedStore: LastPlaybackSpeedStore,
  private val scope: CoroutineScope,
  private val volumeGain: VolumeGain,
  private val sleepTimer: SleepTimer,
  private val analytics: Analytics,
  private val historyRecorder: ListeningHistoryRecorder,
  private val commandSourceResolver: CommandSourceResolver,
) : ForwardingPlayer(player) {

  private var skipSilence = false

  private val endOfChapterSleepTimerListener = object : Player.Listener {
    override fun onMediaItemTransition(
      mediaItem: MediaItem?,
      reason: Int,
    ) {
      player.setSkipSilenceEnabled(skipSilence)
    }

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
      sleepTimer.disable()
      player.pause()
      player.playbackPosition()?.let { position ->
        historyRecorder.record(
          type = ListeningEvent.Type.SleepTimerEnded,
          source = ListeningEvent.Source.SleepTimer,
          position = position,
        )
      }
    }
  }

  /**
   * Rewinds when playback actually paused, so a pause command while already paused doesn't rewind again, and the
   * pauses ExoPlayer does on its own rewind as well.
   */
  private val autoRewindListener = object : Player.Listener {
    override fun onPlayWhenReadyChanged(
      playWhenReady: Boolean,
      reason: Int,
    ) {
      if (!playWhenReady && reason in autoRewindReasons) {
        autoRewind()
      }
    }
  }

  init {
    player.addListener(endOfChapterSleepTimerListener)
    player.addListener(autoRewindListener)
    // ExoPlayer adds skipped silence to the position as soon as it processes it, while the audio before it still
    // plays. So the position would reach the end of the chapter, and the sleep timer would pause, before its last
    // words were heard.
    player.onChapterEnding = {
      if (sleepTimer.state.value is SleepTimerState.Enabled.WithEndOfChapter) {
        player.setSkipSilenceEnabled(false)
      }
    }
  }

  // The app changes chapters through custom commands that end up here, because its controller can still hold the book
  // before it was split into chapters.
  fun forceSeekToNext() {
    player.seekToNextMediaItem()
  }

  fun forceSeekToPrevious() {
    player.seekToPrevious()
  }

  // Next and previous from headsets, cars and watches skip instead of changing the chapter, so listeners keep their
  // place.
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
    val source = commandSourceResolver.current()
    val position = player.playbackPosition()
    scope.launch {
      val skipAmount = seekTimeStore.data.first().seconds
      recordExternalCommand(source, ListeningEvent.Type.SkipBack, position)
      seekBackBy(skipAmount)
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
      currentPosition = previousMediaItem.mediaMetadata.durationMs?.milliseconds ?: return
      mediaItemIndex = previousMediaItemIndex
    }

    player.seekTo(mediaItemIndex, (currentPosition - remaining).inWholeMilliseconds)
  }

  override fun seekForward() {
    val source = commandSourceResolver.current()
    val position = player.playbackPosition()
    scope.launch {
      val skipAmount = seekTimeStore.data.first().seconds
      recordExternalCommand(source, ListeningEvent.Type.SkipForward, position)

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

  override fun seekTo(positionMs: Long) {
    recordExternalSeek(currentMediaItemIndex, positionMs)
    super.seekTo(positionMs)
  }

  override fun seekTo(
    mediaItemIndex: Int,
    positionMs: Long,
  ) {
    recordExternalSeek(mediaItemIndex, positionMs)
    super.seekTo(mediaItemIndex, positionMs)
  }

  override fun seekToDefaultPosition(mediaItemIndex: Int) {
    recordExternalSeek(mediaItemIndex, 0)
    super.seekToDefaultPosition(mediaItemIndex)
  }

  private fun recordExternalSeek(
    mediaItemIndex: Int,
    positionMs: Long,
  ) {
    val source = commandSourceResolver.current() ?: return
    val type = if (mediaItemIndex == currentMediaItemIndex) {
      ListeningEvent.Type.Seek
    } else {
      ListeningEvent.Type.ChapterChange
    }
    recordExternalCommand(
      source = source,
      type = type,
      position = player.playbackPosition(),
      to = player.playbackPosition(mediaItemIndex, positionMs),
    )
  }

  private fun recordExternalCommand(
    source: CommandSource?,
    type: ListeningEvent.Type,
    position: PlaybackPosition?,
    to: PlaybackPosition? = null,
  ) {
    if (source == null || position == null) return
    historyRecorder.record(
      type = type,
      source = source.source,
      position = position,
      to = to,
    )
  }

  override fun play() {
    playWhenReady = true
  }

  override fun setPlayWhenReady(playWhenReady: Boolean) {
    Logger.d("setPlayWhenReady=$playWhenReady")
    if (playWhenReady != getPlayWhenReady()) {
      recordExternalCommand(
        source = commandSourceResolver.current(),
        type = if (playWhenReady) ListeningEvent.Type.Play else ListeningEvent.Type.Pause,
        position = player.playbackPosition(),
      )
    }

    if (playWhenReady) {
      analytics.event("play", mapOf("volume_boost" to if (volumeGain.gain > Decibel.Zero) "on" else "off"))
      updateLastPlayedAt()
      rememberSpeed(playbackParameters.speed)
    } else {
      analytics.event("pause")
    }
    super.setPlayWhenReady(playWhenReady)
  }

  private fun autoRewind() {
    // a finished book stays at its end
    if (player.playbackState == STATE_ENDED) return
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

  override fun pause() {
    playWhenReady = false
  }

  private fun updateLastPlayedAt() {
    // the book that plays, which can differ from the current book until the app loads that one
    val bookId = player.currentMediaItem?.mediaId?.toMediaIdOrNull()?.bookId ?: return
    scope.launch {
      repo.updateBook(bookId) {
        it.copy(lastPlayedAt = Instant.now())
      }
    }
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
    val mediaId = mediaItem.mediaId.toMediaIdOrNull()
    if (mediaId != null) {
      if (mediaId is MediaId.Book) {
        val book = runBlocking {
          repo.get(mediaId.id)
        }
        if (book != null) {
          val speed = book.withDefaultSpeed { runBlocking { lastPlaybackSpeedStore.data.first() } }.content.playbackSpeed
          player.setPlaybackSpeed(speed)
          if (speed != book.content.playbackSpeed) {
            // loading the book marks it as played, so from here on this is its own speed
            scope.launch {
              repo.updateBook(book.id) { it.copy(playbackSpeed = speed) }
            }
          }
          setSkipSilenceEnabled(book.content.skipSilence)
          volumeGain.gain = Decibel(book.content.gain)
          val currentPlaybackItem = book.playbackItemForPosition(
            chapterId = book.content.currentChapter,
            positionInChapterMs = book.content.positionInChapter,
          ) ?: return
          player.setBook(
            book = book,
            startItemIndex = currentPlaybackItem.index,
            positionInItemMs = currentPlaybackItem.positionInMediaItem(book.content.positionInChapter),
          )
        }
      } else {
        Logger.w("Unexpected media id type ${mediaId::class.simpleName}")
      }
    }
  }

  override fun setPlaybackSpeed(speed: Float) {
    super.setPlaybackSpeed(speed)
    scope.launch {
      updateBook { it.copy(playbackSpeed = speed) }
    }
    rememberSpeed(speed)
  }

  /**
   * Books that were never played start at this speed, see [withDefaultSpeed].
   */
  private fun rememberSpeed(speed: Float) {
    scope.launch {
      lastPlaybackSpeedStore.updateData { speed }
    }
  }

  fun setSkipSilenceEnabled(enabled: Boolean) {
    skipSilence = enabled
    scope.launch {
      updateBook { it.copy(skipSilence = enabled) }
    }
    player.setSkipSilenceEnabled(enabled)
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

private val autoRewindReasons = setOf(
  Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
  Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
  Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY,
)

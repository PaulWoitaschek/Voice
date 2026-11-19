package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.analytics.api.Analytics
import voice.core.data.BookContent
import voice.core.data.ListeningEvent
import voice.core.data.isRemote
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
import voice.core.playback.nextChapterTarget
import voice.core.playback.session.bookId
import voice.core.playback.session.currentBookId
import voice.core.playback.session.toMediaIdOrNull
import voice.core.playback.stats.ListeningSessionRecorder
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerState

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
  private val listeningSessionRecorder: ListeningSessionRecorder,
) : ForwardingPlayer(player) {

  private var skipSilence = false

  private val endOfChapterSleepTimerListener =
    object : Player.Listener {
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
   * Rewinds when playback actually paused, so a pause command while already paused doesn't rewind
   * again, and the pauses ExoPlayer does on its own rewind as well.
   */
  private val autoRewindListener =
    object : Player.Listener {
      override fun onPlayWhenReadyChanged(
        playWhenReady: Boolean,
        reason: Int,
      ) {
        if (!playWhenReady && reason in autoRewindReasons) {
          autoRewind()
        }
      }
    }

  // At the end of the book, after an error or with another book, play starts playback although
  // the player may still want to play.
  private var stopped = false

  private val stoppedListener =
    object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
          STATE_ENDED,
          STATE_IDLE -> stopped = true
          STATE_READY -> stopped = false
          STATE_BUFFERING -> Unit
        }
      }
    }

  init {
    player.addListener(endOfChapterSleepTimerListener)
    player.addListener(autoRewindListener)
    player.addListener(stoppedListener)
    // ExoPlayer adds skipped silence to the position as soon as it processes it, while the audio
    // before it still
    // plays. So the position would reach the end of the chapter, and the sleep timer would pause,
    // before its last
    // words were heard.
    player.onChapterEnding = {
      if (sleepTimer.state.value is SleepTimerState.Enabled.WithEndOfChapter) {
        player.setSkipSilenceEnabled(false)
      }
    }
  }

  // Next and previous from headsets, cars and watches skip instead of changing the chapter, so
  // listeners keep their
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
      val skipAmount = seekTimeStore.data.first()
      recordExternalCommand(source, ListeningEvent.Type.SkipBack, position)
      player.seekBackBy(skipAmount, crossMediaItems = true)
    }
  }

  override fun seekForward() {
    val source = commandSourceResolver.current()
    val position = player.playbackPosition()
    scope.launch {
      val skipAmount = seekTimeStore.data.first()
      recordExternalCommand(source, ListeningEvent.Type.SkipForward, position)

      val currentPosition =
        player.currentPosition.takeUnless { it == C.TIME_UNSET }?.milliseconds?.coerceAtLeast(ZERO)
          ?: return@launch
      val newPosition = currentPosition + skipAmount

      val duration =
        player.duration.takeUnless { it == C.TIME_UNSET }?.milliseconds ?: return@launch

      if (newPosition > duration) {
        if (nextMediaItemIndex == C.INDEX_UNSET) {
          val (index, endMs) = player.nextChapterTarget() ?: return@launch
          player.seekTo(index, endMs)
        } else {
          player.seekTo(
            nextMediaItemIndex,
            (duration - newPosition).absoluteValue.inWholeMilliseconds,
          )
        }
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
    val type =
      if (mediaItemIndex == currentMediaItemIndex) {
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

  override fun setMediaItems(
    mediaItems: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
  ) {
    if (mediaItems.firstOrNull()?.mediaId?.toMediaIdOrNull()?.bookId != player.currentBookId()) {
      stopped = true
    }
    super.setMediaItems(mediaItems, startIndex, startPositionMs)
  }

  override fun setPlayWhenReady(playWhenReady: Boolean) {
    Logger.d("setPlayWhenReady=$playWhenReady")
    val source = commandSourceResolver.current()
    if (playWhenReady != getPlayWhenReady()) {
      recordExternalCommand(
        source = source,
        type = if (playWhenReady) ListeningEvent.Type.Play else ListeningEvent.Type.Pause,
        position = player.playbackPosition(),
      )
    }
    if (playWhenReady && (!getPlayWhenReady() || stopped)) {
      stopped = false
      trackPlay(source)
    }

    if (playWhenReady) {
      updateLastPlayedAt()
      rememberSpeed(playbackParameters.speed)
    } else {
      analytics.event("pause")
    }
    super.setPlayWhenReady(playWhenReady)
  }

  private fun trackPlay(commandSource: CommandSource?) {
    // without a book, play from a headset or a car still reaches the player, but nothing plays
    val bookId = player.currentBookId() ?: return
    // the app's own controllers resolve to no source, but record theirs right before they play
    val source = commandSource?.source ?: historyRecorder.lastSource
    listeningSessionRecorder.playPressed(source)
    analytics.event(
      "play",
      mapOf(
        "volume_boost" to (volumeGain.gain > Decibel.Zero).onOff(),
        "skip_silence" to skipSilence.onOff(),
        "speed" to speedRange(playbackParameters.speed),
        "source" to source.analyticsName(),
        "book_from" to if (bookId.isRemote) "audiobookshelf" else "folder",
      ),
    )
  }

  private fun autoRewind() {
    // a finished book stays at its end
    if (player.playbackState == STATE_ENDED) return
    val currentPosition =
      player.currentPosition.takeUnless { it == C.TIME_UNSET }?.milliseconds ?: ZERO
    if (currentPosition > ZERO) {
      scope.launch {
        player.seekBackBy(
          rewind = autoRewindAmountStore.data.first(),
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
    val bookId = player.currentBookId() ?: return
    scope.launch {
      repo.updateBook(bookId) {
        it.copy(lastPlayedAt = Clock.System.now())
      }
    }
  }

  /**
   * Applies the settings of the book that is about to play. Unlike the setters, it doesn't store
   * them, as they already are the book's own.
   */
  fun applySettings(content: BookContent) {
    player.setPlaybackSpeed(content.playbackSpeed)
    skipSilence = content.skipSilence
    player.setSkipSilenceEnabled(content.skipSilence)
    volumeGain.gain = Decibel(content.gain)
  }

  override fun setPlaybackSpeed(speed: Float) {
    super.setPlaybackSpeed(speed)
    scope.launch {
      updateBook { it.copy(playbackSpeed = speed) }
    }
    rememberSpeed(speed)
  }

  /** Books that were never played start at this speed, see [withDefaultSpeed]. */
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

private val autoRewindReasons =
  setOf(
    Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY,
  )

private fun Boolean.onOff(): String = if (this) "on" else "off"

private fun speedRange(speed: Float): String =
  when {
    speed < 1F -> "slower"
    speed == 1F -> "1x"
    speed <= 1.5F -> "up_to_1_5x"
    else -> "faster"
  }

// the listening counts on the phone are stored under these names too, so they must stay
internal fun ListeningEvent.Source?.analyticsName(): String =
  when (this) {
    ListeningEvent.Source.Widget -> "widget"
    ListeningEvent.Source.Notification -> "notification"
    ListeningEvent.Source.Headset -> "headset"
    ListeningEvent.Source.Bluetooth -> "bluetooth"
    ListeningEvent.Source.Car -> "car"
    ListeningEvent.Source.Watch -> "watch"
    ListeningEvent.Source.OtherApp,
    ListeningEvent.Source.Unknown -> "other_app"
    ListeningEvent.Source.SleepTimer -> "sleep_timer"
    ListeningEvent.Source.App,
    ListeningEvent.Source.AudioFocus,
    ListeningEvent.Source.Unplugged,
    ListeningEvent.Source.OtherDevice,
    null -> "app"
  }

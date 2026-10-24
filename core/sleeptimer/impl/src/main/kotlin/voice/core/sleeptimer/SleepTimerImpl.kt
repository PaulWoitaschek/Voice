package voice.core.sleeptimer

import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.ListeningEvent
import voice.core.logging.api.Logger
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.playstate.PlayStateManager.PlayState.Playing
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SleepTimerImpl internal constructor(
  private val playStateManager: PlayStateManager,
  private val shakeDetector: ShakeDetector,
  private val sleepTimerPreferenceStore: SleepTimerPreferenceStore,
  private val playerController: PlayerController,
  private val fadeOutStore: FadeOutStore,
  dispatcherProvider: DispatcherProvider,
  private val tracker: SleepTimerTracker,
) : SleepTimer {

  private val scope = MainScope(dispatcherProvider)
  override val state: StateFlow<SleepTimerState>
    field = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)

  private var job: Job? = null

  override fun enable(mode: SleepTimerMode) {
    tracker.enabled(mode)
    disable() // cancel any active job first
    playerController.record(
      ListeningEvent.Type.SleepTimerStarted,
      if (mode == SleepTimerMode.TimedWithDefault) ListeningEvent.Source.SleepTimer else ListeningEvent.Source.App,
    )

    job = scope.launch {
      when (mode) {
        is SleepTimerMode.TimedWithDuration -> startCountdown(mode.duration)
        SleepTimerMode.TimedWithDefault -> {
          // the default duration is only used when the sleep timer turns on by itself
          startCountdown(sleepTimerPreferenceStore.data.first().duration)
        }
        SleepTimerMode.EndOfChapter -> state.value = SleepTimerState.Enabled.WithEndOfChapter
      }
    }
  }

  override fun disable() {
    tracker.disabled()
    job?.cancel()
    job = null
    state.value = SleepTimerState.Disabled
    playerController.setVolume(1F)
  }

  private tailrec suspend fun startCountdown(duration: Duration) {
    Logger.d("startCountdown(duration=$duration)")
    var left = duration
    state.value = SleepTimerState.Enabled.WithDuration(left)
    playerController.setVolume(1F)

    val fadeOutDuration = fadeOutStore.data.first()
    var interval = 500.milliseconds

    while (left > Duration.ZERO) {
      suspendUntilPlaying()
      if (left < fadeOutDuration) {
        interval = 200.milliseconds
        updateVolume(left, fadeOutDuration)
      }
      delay(interval)
      left = max((left - interval).inWholeMilliseconds, 0).milliseconds
      state.value = SleepTimerState.Enabled.WithDuration(left)
    }
    playerController.setVolume(1f)
    state.value = SleepTimerState.Disabled

    playerController.record(ListeningEvent.Type.SleepTimerEnded, ListeningEvent.Source.SleepTimer)
    playerController.pauseWithRewind(fadeOutDuration)

    val shakeDetected = detectShakeWithTimeout()
    playerController.setVolume(1F)
    if (shakeDetected) {
      Logger.i("Shake detected, resetting timer")
      playerController.record(ListeningEvent.Type.SleepTimerExtended, ListeningEvent.Source.SleepTimer)
      playerController.play(source = null)
      startCountdown(duration)
    }
  }

  private suspend fun detectShakeWithTimeout(): Boolean {
    return withTimeoutOrNull(SHAKE_TO_RESET_TIME) {
      shakeDetector.detect()
      true
    } ?: false
  }

  private fun updateVolume(
    left: Duration,
    fadeOutDuration: Duration,
  ) {
    val percentage = (left / fadeOutDuration).toFloat().coerceIn(0f, 1f)
    val volume = 1 - FastOutSlowInInterpolator().getInterpolation(1 - percentage)
    playerController.setVolume(volume)
  }

  private suspend fun suspendUntilPlaying() {
    if (playStateManager.playState != Playing) {
      playStateManager.playStateFlow.first { it == Playing }
    }
  }

  internal companion object {
    val SHAKE_TO_RESET_TIME = 30.seconds
  }
}

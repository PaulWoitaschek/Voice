package voice.core.sleeptimer

import androidx.datastore.core.DataStore
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
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
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.data.store.FadeOutStore
import voice.core.data.store.SleepTimerPreferenceStore
import voice.core.logging.core.Logger
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.playstate.PlayStateManager.PlayState.Playing
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class SleepTimerImpl(
  private val playStateManager: PlayStateManager,
  private val shakeDetector: ShakeDetector,
  @SleepTimerPreferenceStore
  private val sleepTimerPreferenceStore: DataStore<SleepTimerPreference>,
  private val playerController: PlayerController,
  @FadeOutStore
  private val fadeOutStore: DataStore<Duration>,
  dispatcherProvider: DispatcherProvider,
) : SleepTimer {

  private val scope = MainScope(dispatcherProvider)
  private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)
  override val state: StateFlow<SleepTimerState> get() = _state

  private var job: Job? = null

  override fun enable(mode: SleepTimerMode) {
    disable() // cancel any active job first

    job = scope.launch {
      when (mode) {
        is SleepTimerMode.TimedWithDuration -> startCountdown(mode.duration)
        SleepTimerMode.TimedWithDefault -> {
          val pref = sleepTimerPreferenceStore.data.first()
          startCountdown(pref.duration)
        }
        SleepTimerMode.EndOfChapter -> {
          _state.value = SleepTimerState.Enabled.WithEndOfChapter
        }
      }
    }
  }

  override fun disable() {
    job?.cancel()
    job = null
    _state.value = SleepTimerState.Disabled
    playerController.setVolume(1F)
  }

  private suspend fun startCountdown(duration: Duration) {
    Logger.d("startCountdown(duration=$duration)")
    var left = duration
    _state.value = SleepTimerState.Enabled.WithDuration(left)
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
      _state.value = SleepTimerState.Enabled.WithDuration(left)
    }
    playerController.setVolume(1f)
    _state.value = SleepTimerState.Disabled

    playerController.pauseWithRewind(fadeOutDuration)

    val shakeToResetTime = 30.seconds
    Logger.d("Waiting $shakeToResetTime for shake...")
    withTimeoutOrNull(shakeToResetTime) {
      shakeDetector.detect()
      Logger.i("Shake detected, resetting timer")
      playerController.play()
      startCountdown(duration)
    }
    playerController.setVolume(1F)
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
      Logger.i("Not playing. Waiting for playback to continue.")
      playStateManager.flow.first { it == Playing }
      Logger.i("Playback resumed.")
    }
  }
}

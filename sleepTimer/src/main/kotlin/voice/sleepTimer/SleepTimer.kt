package voice.sleepTimer

import androidx.datastore.core.DataStore
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import voice.common.AppScope
import voice.common.autoSleepTimer.AutoSleepTimer
import voice.common.pref.AutoSleepTimerStore
import voice.common.pref.FadeOutStore
import voice.common.pref.SleepTimeStore
import voice.logging.core.Logger
import voice.playback.PlayerController
import voice.playback.playstate.PlayStateManager
import voice.playback.playstate.PlayStateManager.PlayState.Playing
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import voice.playback.session.SleepTimer as PlaybackSleepTimer

@Singleton
@ContributesBinding(AppScope::class)
class SleepTimer
@Inject constructor(
  private val playStateManager: PlayStateManager,
  private val shakeDetector: ShakeDetector,
  @SleepTimeStore
  private val sleepTimeStore: DataStore<Duration>,
  private val playerController: PlayerController,
  @FadeOutStore
  private val fadeOutStore: DataStore<Duration>,
  @AutoSleepTimerStore
  private val autoSleepTimerStore: DataStore<AutoSleepTimer>,
) : PlaybackSleepTimer {

  private val scope = MainScope()

  private val _leftSleepTime = MutableStateFlow(Duration.ZERO)
  private var leftSleepTime: Duration
    get() = _leftSleepTime.value
    set(value) {
      _leftSleepTime.value = value
    }
  override val leftSleepTimeFlow: StateFlow<Duration> get() = _leftSleepTime

  override fun sleepTimerActive(): Boolean = sleepJob?.isActive == true && leftSleepTime > Duration.ZERO

  private var sleepJob: Job? = null

  init {
    scope.launch {
      combine(
        playStateManager.flow,
        autoSleepTimerStore.data,
      ) { playState, autoSleepTimer ->
        playState to autoSleepTimer
      }.collect { (playState, autoSleepTimer) ->
        if (
          playState == Playing &&
          autoSleepTimer.enabled &&
          isTimeInRange(LocalTime.now(), autoSleepTimer.startTime, autoSleepTimer.endTime)
        ) {
          setActive()
        }
      }
    }
  }

  override fun setActive(enable: Boolean) {
    Logger.i("enable=$enable")
    if (enable) {
      setActive()
    } else {
      cancel()
    }
  }

  private fun setActive() {
    scope.launch {
      setActive(sleepTimeStore.data.first())
    }
  }

  fun setActive(sleepTime: Duration) {
    Logger.i("Starting sleepTimer. Pause in $sleepTime.")
    leftSleepTime = sleepTime
    playerController.setVolume(1F)
    sleepJob?.cancel()
    sleepJob = scope.launch {
      startSleepTimerCountdown()
      val shakeToResetTime = 30.seconds
      Logger.d("Wait for $shakeToResetTime for a shake")
      withTimeout(shakeToResetTime) {
        shakeDetector.detect()
        Logger.i("Shake detected. Reset sleep time")
        playerController.play()
        setActive(sleepTime)
      }
      Logger.i("exiting")
    }
  }

  private suspend fun startSleepTimerCountdown() {
    var interval = 500.milliseconds
    val fadeOutDuration = fadeOutStore.data.first()
    while (leftSleepTime > Duration.ZERO) {
      suspendUntilPlaying()
      if (leftSleepTime < fadeOutDuration) {
        interval = 200.milliseconds
        updateVolumeForSleepTime(fadeOutDuration)
      }
      delay(interval)
      leftSleepTime = (leftSleepTime - interval).coerceAtLeast(Duration.ZERO)
    }
    playerController.pauseWithRewind(fadeOutDuration)
    playerController.setVolume(1F)
  }

  private fun updateVolumeForSleepTime(fadeOutDuration: Duration) {
    val percentageOfTimeLeft = if (leftSleepTime == Duration.ZERO) {
      0F
    } else {
      (leftSleepTime / fadeOutDuration).toFloat()
    }.coerceIn(0F, 1F)

    val volume = 1 - FastOutSlowInInterpolator().getInterpolation(1 - percentageOfTimeLeft)
    playerController.setVolume(volume)
  }

  private suspend fun suspendUntilPlaying() {
    if (playStateManager.playState != Playing) {
      Logger.i("Not playing. Wait for Playback to continue.")
      playStateManager.flow
        .first { it == Playing }
      Logger.i("Playback continued.")
    }
  }

  private fun cancel() {
    sleepJob?.cancel()
    leftSleepTime = Duration.ZERO
    playerController.setVolume(1F)
  }
}

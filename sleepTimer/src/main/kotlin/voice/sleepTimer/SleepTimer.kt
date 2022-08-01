package voice.sleepTimer

import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import de.paulwoitaschek.flowpref.Pref
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import voice.common.pref.PrefKeys
import voice.logging.core.Logger
import voice.playback.PlayerController
import voice.playback.playstate.PlayStateManager
import voice.playback.playstate.PlayStateManager.PlayState.Playing
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Singleton
class SleepTimer
@Inject constructor(
  private val playStateManager: PlayStateManager,
  private val shakeDetector: ShakeDetector,
  @Named(PrefKeys.SLEEP_TIME)
  private val sleepTimePref: Pref<Int>,
  private val playerController: PlayerController,
) {

  private val scope = MainScope()
  private val sleepTime: Duration get() = sleepTimePref.value.minutes
  private val fadeOutDuration = 10.seconds

  private val _leftSleepTime = MutableStateFlow(Duration.ZERO)
  private var leftSleepTime: Duration
    get() = _leftSleepTime.value
    set(value) {
      _leftSleepTime.value = value
    }
  val leftSleepTimeFlow: Flow<Duration> get() = _leftSleepTime

  fun sleepTimerActive(): Boolean = sleepJob?.isActive == true && leftSleepTime > Duration.ZERO

  private var sleepJob: Job? = null

  fun setActive(enable: Boolean) {
    Logger.i("enable=$enable")
    if (enable) {
      start()
    } else {
      cancel()
    }
  }

  private fun start() {
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
        start()
      }
      Logger.i("exiting")
    }
  }

  private suspend fun startSleepTimerCountdown() {
    var interval = 500.milliseconds
    while (leftSleepTime > Duration.ZERO) {
      suspendUntilPlaying()
      if (leftSleepTime < fadeOutDuration) {
        interval = 200.milliseconds
        updateVolumeForSleepTime()
      }
      delay(interval)
      leftSleepTime = (leftSleepTime - interval).coerceAtLeast(Duration.ZERO)
    }
    playerController.pauseWithRewind(fadeOutDuration)
    playerController.setVolume(1F)
  }

  private fun updateVolumeForSleepTime() {
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
        .filter { it == Playing }
        .first()
      Logger.i("Playback continued.")
    }
  }

  private fun cancel() {
    sleepJob?.cancel()
    leftSleepTime = Duration.ZERO
    playerController.setVolume(1F)
  }
}

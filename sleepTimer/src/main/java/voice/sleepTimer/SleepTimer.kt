package voice.sleepTimer

import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.playback.playstate.PlayStateManager.PlayState.Playing
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.minutes
import kotlin.time.seconds

@Singleton
class SleepTimer
@Inject constructor(
  private val playStateManager: PlayStateManager,
  private val shakeDetector: ShakeDetector,
  @Named(PrefKeys.SLEEP_TIME)
  private val sleepTimePref: Pref<Int>,
  private val playerController: PlayerController
) {

  private val scope = MainScope()
  private val sleepTime: Duration get() = sleepTimePref.value.minutes

  private val _leftSleepTime = ConflatedBroadcastChannel(Duration.ZERO)
  private var leftSleepTime: Duration
    get() = _leftSleepTime.value
    set(value) {
      _leftSleepTime.trySend(value)
    }
  val leftSleepTimeFlow: Flow<Duration> get() = _leftSleepTime.asFlow()

  fun sleepTimerActive(): Boolean = sleepJob?.isActive == true && leftSleepTime > Duration.ZERO

  private var sleepJob: Job? = null

  fun setActive(enable: Boolean) {
    Timber.i("enable=$enable")
    if (enable) {
      start()
    } else {
      cancel()
    }
  }

  private fun start() {
    Timber.i("Starting sleepTimer. Pause in $sleepTime.")
    leftSleepTime = sleepTime
    sleepJob?.cancel()
    sleepJob = scope.launch {
      startSleepTimerCountdown()
      val shakeToResetTime = 30.seconds
      Timber.d("Wait for $shakeToResetTime for a shake")
      withTimeout(shakeToResetTime) {
        shakeDetector.detect().first()
        Timber.i("Shake detected. Reset sleep time")
        playerController.play()
        start()
      }
      Timber.i("exiting")
    }
  }

  private suspend fun startSleepTimerCountdown() {
    val interval = 500.milliseconds
    while (leftSleepTime > Duration.ZERO) {
      suspendUntilPlaying()
      delay(interval)
      leftSleepTime = (leftSleepTime - interval).coerceAtLeast(Duration.ZERO)
    }
    playerController.pause()
  }

  private suspend fun suspendUntilPlaying() {
    if (playStateManager.playState != Playing) {
      Timber.i("Not playing. Wait for Playback to continue.")
      playStateManager.playStateFlow()
        .filter { it == Playing }
        .first()
      Timber.i("Playback continued.")
    }
  }

  private fun cancel() {
    sleepJob?.cancel()
    leftSleepTime = Duration.ZERO
  }
}

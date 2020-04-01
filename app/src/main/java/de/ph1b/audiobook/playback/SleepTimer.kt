package de.ph1b.audiobook.playback

import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.delay
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.playback.player.FADE_OUT_DURATION
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.playback.playstate.PlayStateManager.PlayState.Playing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.minutes

@Singleton
class SleepTimer
@Inject constructor(
  private val playStateManager: PlayStateManager,
  private val shakeDetector: ShakeDetector,
  @Named(PrefKeys.SHAKE_TO_RESET)
  shakeToResetPref: Pref<Boolean>,
  @Named(PrefKeys.SLEEP_TIME)
  private val sleepTimePref: Pref<Int>,
  private val playerController: PlayerController
) {

  private val shakeToResetEnabled by shakeToResetPref
  private val sleepTime: Duration get() = sleepTimePref.value.minutes

  private val _leftSleepTime = ConflatedBroadcastChannel(Duration.ZERO)
  private var leftSleepTime: Duration
    get() = _leftSleepTime.value
    set(value) {
      _leftSleepTime.offer(value)
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
    Timber.i("Starting sleepTimer. Sleep in ${sleepTime.inMinutes.toLong()} minutes.")
    leftSleepTime = sleepTime
    sleepJob?.cancel()
    sleepJob = GlobalScope.launch(Dispatchers.Main) {
      launch { restartTimerOnShake() }
      startSleepTimerCountdown()
      if (shakeToResetEnabled) {
        Timber.i("sleep timer ended. Wait for another minute while listening for shakes.")
        delay(1.minutes)
      }
      Timber.i("exiting")
    }
  }

  private suspend fun startSleepTimerCountdown() {
    val interval = 500.milliseconds
    var fadeOutSent = false
    while (leftSleepTime > Duration.ZERO) {
      suspendUntilPlaying()
      delay(interval)
      leftSleepTime = (leftSleepTime - interval).coerceAtLeast(Duration.ZERO)
      if (leftSleepTime <= FADE_OUT_DURATION && !fadeOutSent) {
        fadeOutSent = true
        playerController.fadeOut()
      }
    }
  }

  private suspend fun restartTimerOnShake() {
    if (shakeToResetEnabled) {
      shakeDetector.detect()
        .collect {
          Timber.i("Shake detected. Reset sleep time")
          if (leftSleepTime > Duration.ZERO) {
            playerController.cancelFadeout()
            leftSleepTime = sleepTime
          } else {
            playerController.play()
            start()
          }
        }
    }
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
    playerController.cancelFadeout()
  }
}

package de.ph1b.audiobook.playback

import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PLAYING
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitFirst
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private val FADE_OUT_MS = SECONDS.toMillis(10)

@Singleton
class SleepTimer
@Inject constructor(
  private val mediaPlayer: MediaPlayer,
  private val playStateManager: PlayStateManager,
  private val shakeDetector: ShakeDetector,
  @Named(PrefKeys.SHAKE_TO_RESET)
  private val shakeToResetPref: Pref<Boolean>,
  @Named(PrefKeys.SLEEP_TIME)
  private val sleepTimePref: Pref<Int>
) {

  private val _leftSleepTimeInMsSubject = BehaviorSubject.createDefault<Long>(0)
  private var leftSleepTimeMs: Long
    get() = _leftSleepTimeInMsSubject.value!!
    set(value) {
      _leftSleepTimeInMsSubject.onNext(value)
    }
  val leftSleepTimeInMsStream: Observable<Long> =
    _leftSleepTimeInMsSubject.observeOn(AndroidSchedulers.mainThread())

  fun sleepTimerActive(): Boolean = sleepJob?.isActive == true

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
    Timber.i("Starting sleepTimer. Sleep in ${sleepTimePref.value} minutes.")
    val sleepTimeInMs = MINUTES.toMillis(sleepTimePref.value.toLong())
    leftSleepTimeMs = sleepTimeInMs
    sleepJob?.cancel()
    sleepJob = GlobalScope.launch {
      val shakeDetectorJob = launch { restartTimerOnShake() }
      startSleepTimerCountdown()
      if (leftSleepTimeMs == 0L) {
        stopPlayerForTimerEnded()
      }
      Timber.i("sleep timer ended")
      if (isActive) {
        continueListeningToShakesForSomeTime(shakeDetectorJob)
      }
      Timber.i("exiting")
    }
  }

  private suspend fun continueListeningToShakesForSomeTime(shakeDetectorJob: Job) {
    val listenToShakesForMs = MINUTES.toMillis(1)
    Timber.i("Not cancelled. Continue listening to the shake detector for $listenToShakesForMs ms")
    delay(listenToShakesForMs)
    shakeDetectorJob.cancel()
  }

  private suspend fun startSleepTimerCountdown() {
    val interval = 500L
    while (leftSleepTimeMs > 0) {
      delay(interval)
      suspendUntilPlaying()
      leftSleepTimeMs = (leftSleepTimeMs - interval).coerceAtLeast(0)
      if (leftSleepTimeMs <= FADE_OUT_MS) {
        adjustPlayerVolumeForSleepTime()
      }
    }
  }

  private suspend fun stopPlayerForTimerEnded() {
    withContext(Dispatchers.Main) {
      mediaPlayer.skip(-FADE_OUT_MS, MILLISECONDS)
      mediaPlayer.stop()
      mediaPlayer.setVolume(1F)
    }
  }

  private suspend fun adjustPlayerVolumeForSleepTime() {
    val volume = leftSleepTimeMs.toFloat() / FADE_OUT_MS
    Timber.i("set volume to $volume")
    withContext(Dispatchers.Main) {
      mediaPlayer.setVolume(volume)
    }
  }

  private suspend fun restartTimerOnShake() {
    if (shakeToResetPref.value) {
      @Suppress("EXPERIMENTAL_API_USAGE")
      shakeDetector.detect()
        .consumeEach {
          Timber.i("Shake detected. Reset sleep time")
          if (leftSleepTimeMs > 0) {
            withContext(Dispatchers.Main) {
              mediaPlayer.setVolume(1F)
            }
            leftSleepTimeMs = MINUTES.toMillis(sleepTimePref.value.toLong())
          } else {
            withContext(Dispatchers.Main) {
              mediaPlayer.play()
            }
            start()
          }
        }
    }
  }

  private suspend fun suspendUntilPlaying() {
    if (playStateManager.playState != PLAYING) {
      Timber.i("Not playing. Wait for Playback to continue.")
      playStateManager.playStateStream()
        .filter { it == PLAYING }
        .awaitFirst()
      Timber.i("Playback continued.")
    }
  }

  private fun cancel() {
    sleepJob?.cancel()
    _leftSleepTimeInMsSubject.onNext(0)
    mediaPlayer.setVolume(1F)
  }
}

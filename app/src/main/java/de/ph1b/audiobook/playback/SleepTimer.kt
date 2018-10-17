package de.ph1b.audiobook.playback

import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val NOT_ACTIVE = -1L

@Singleton
class SleepTimer
@Inject constructor(
  private val mediaPlayer: MediaPlayer,
  playStateManager: PlayStateManager,
  shakeDetector: ShakeDetector,
  @Named(PrefKeys.SHAKE_TO_RESET)
  private val shakeToResetPref: Pref<Boolean>,
  @Named(PrefKeys.SLEEP_TIME)
  private val sleepTimePref: Pref<Int>
) {

  private val _leftSleepTimeInMs = BehaviorSubject.createDefault<Long>(NOT_ACTIVE)
  val leftSleepTimeInMs: Observable<Long> = _leftSleepTimeInMs
  private var shakeDisposable: Disposable? = null
  private var shakeTimeoutDisposable: Disposable? = null
  private val shakeObservable = shakeDetector.detect()

  init {
    val fadeOutMs = SECONDS.toMillis(10)
    @Suppress("CheckResult")
    _leftSleepTimeInMs.filter { it <= fadeOutMs }
      .subscribe { msLeft ->
        if (msLeft == 0L) {
          mediaPlayer.pause(rewind = false)
          mediaPlayer.skip(-fadeOutMs, MILLISECONDS)
          mediaPlayer.stop()
        }
        val volume = if (msLeft == 0L || msLeft == NOT_ACTIVE) {
          1F
        } else {
          msLeft.toFloat() / fadeOutMs
        }
        mediaPlayer.setVolume(volume)
      }

    @Suppress("CheckResult")
    _leftSleepTimeInMs.subscribe {
      when {
        it > 0 -> {
          resetTimerOnShake(true)
        }
        it == NOT_ACTIVE -> {
          // if the track ended by the user, disable the shake detector
          resetTimerOnShake(false)
        }
        it == 0L -> {
          // if the timer stopped normally, setup a timer of 5 minutes to resume playback
          resetTimerOnShake(true, 5)
        }
      }
    }

    // counts down the sleep sand
    @Suppress("CheckResult")
    playStateManager.playStateStream()
      .map { it == PlayStateManager.PlayState.PLAYING }
      .distinctUntilChanged()
      .switchMap { playing ->
        val sleepUpdateInterval = 1000L
        if (playing) {
          Observable
            .interval(sleepUpdateInterval, MILLISECONDS, AndroidSchedulers.mainThread())
            .filter { _leftSleepTimeInMs.value!! > 0 } // only notify if there is still time left
            .map { _leftSleepTimeInMs.value!! - sleepUpdateInterval } // calculate the new time
            .map { it.coerceAtLeast(0) }
        } else {
          Observable.empty()
        }
      }
      .subscribe(_leftSleepTimeInMs::onNext)
  }

  /** turns the sleep timer on or off **/
  fun setActive(enable: Boolean) {
    Timber.i("toggleSleepSand. Left sleepTime is ${_leftSleepTimeInMs.value}")

    if (enable) {
      Timber.i("Starting sleepTimer")
      val minutes = sleepTimePref.value
      _leftSleepTimeInMs.onNext(MINUTES.toMillis(minutes.toLong()))
    } else {
      Timber.i("Cancelling sleepTimer")
      _leftSleepTimeInMs.onNext(NOT_ACTIVE)
    }
  }

  private fun resetTimerOnShake(enable: Boolean, stopAfter: Long? = null) {
    if (enable) {
      val shouldSubscribe = shakeDisposable?.isDisposed != false
      if (shouldSubscribe) {
        // setup shake detection if requested
        if (shakeToResetPref.value) {
          shakeDisposable = shakeObservable.subscribe {
            if (_leftSleepTimeInMs.value == 0L) {
              Timber.d("detected shake while sleepSand==0. Resume playback")
              mediaPlayer.play()
            }

            Timber.d("reset now by shake")
            setActive(true)
          }
        }
      }
    } else {
      shakeDisposable?.dispose()
    }

    shakeTimeoutDisposable?.dispose()
    if (stopAfter != null) {
      shakeTimeoutDisposable = Observable.timer(stopAfter, MINUTES)
        .subscribe {
          Timber.d("disabling pauseOnShake through timeout")
          resetTimerOnShake(false)
        }
    }
  }

  fun sleepTimerActive(): Boolean = _leftSleepTimeInMs.value!! > 0
}

package de.ph1b.audiobook.playback

import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Named

private const val NOT_ACTIVE = -1

class SleepTimer(
  private val playerController: PlayerController,
  playStateManager: PlayStateManager,
  shakeDetector: ShakeDetector,
  @Named(PrefKeys.SHAKE_TO_RESET)
  private val shakeToResetPref: Pref<Boolean>,
  @Named(PrefKeys.SLEEP_TIME)
  private val sleepTimePref: Pref<Int>
) {

  private val leftSleepTimeSubject = BehaviorSubject.createDefault<Int>(NOT_ACTIVE)
  private var sleepDisposable: Disposable? = null
  private var shakeDisposable: Disposable? = null
  private var shakeTimeoutDisposable: Disposable? = null
  private val shakeObservable = shakeDetector.detect()

  init {
    @Suppress("CheckResult")
    leftSleepTimeSubject.filter { it == 0 }
      .subscribe { playerController.stop() }

    @Suppress("CheckResult")
    leftSleepTimeSubject.subscribe {
      when {
        it > 0 -> {
          resetTimerOnShake(true)
        }
        it == NOT_ACTIVE -> {
          // if the track ended by the user, disable the shake detector
          resetTimerOnShake(false)
        }
        it == 0 -> {
          // if the timer stopped normally, setup a timer of 5 minutes to resume playback
          resetTimerOnShake(true, 5)
        }
      }
    }

    // counts down the sleep sand
    val sleepUpdateInterval = 1000L
    @Suppress("CheckResult")
    playStateManager.playStateStream()
      .map { it == PlayStateManager.PlayState.PLAYING }
      .distinctUntilChanged()
      .subscribe { playing ->
        if (playing) {
          sleepDisposable = Observable.interval(
            sleepUpdateInterval,
            TimeUnit.MILLISECONDS,
            AndroidSchedulers.mainThread()
          )
            .filter { leftSleepTimeSubject.value!! > 0 } // only notify if there is still time left
            .map { leftSleepTimeSubject.value!! - sleepUpdateInterval } // calculate the new time
            .map { it.coerceAtLeast(0) } // but keep at least 0
            .subscribe { leftSleepTimeSubject.onNext(it.toInt()) }
        } else {
          sleepDisposable?.dispose()
        }
      }
  }

  /** turns the sleep timer on or off **/
  fun setActive(enable: Boolean) {
    Timber.i("toggleSleepSand. Left sleepTime is ${leftSleepTimeSubject.value}")

    if (enable) {
      Timber.i("Starting sleepTimer")
      val minutes = sleepTimePref.value
      leftSleepTimeSubject.onNext(TimeUnit.MINUTES.toMillis(minutes.toLong()).toInt())
    } else {
      Timber.i("Cancelling sleepTimer")
      leftSleepTimeSubject.onNext(NOT_ACTIVE)
    }
  }

  private fun resetTimerOnShake(enable: Boolean, stopAfter: Long? = null) {
    if (enable) {
      val shouldSubscribe = shakeDisposable?.isDisposed != false
      if (shouldSubscribe) {
        // setup shake detection if requested
        if (shakeToResetPref.value) {
          shakeDisposable = shakeObservable.subscribe {
            if (leftSleepTimeSubject.value == 0) {
              Timber.d("detected shake while sleepSand==0. Resume playback")
              playerController.play()
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
      shakeTimeoutDisposable = Observable.timer(stopAfter, TimeUnit.MINUTES)
        .subscribe {
          Timber.d("disabling pauseOnShake through timeout")
          resetTimerOnShake(false)
        }
    }
  }

  val leftSleepTimeInMs: Observable<Int> = leftSleepTimeSubject

  fun sleepTimerActive(): Boolean = leftSleepTimeSubject.value!! > 0
}

package de.ph1b.audiobook.playback

import d
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import i
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages everything sleep related.
 */
@Singleton class Sandman
@Inject constructor(private val playerController: PlayerController, playStateManager: PlayStateManager, private val prefsManager: PrefsManager, shakeDetector: ShakeDetector) {

  /**
   * The time left till the playback stops in ms. If this is -1 the timer was stopped manually.
   * If this is 0 the timer simple counted down.
   */
  private val NOT_ACTIVE = -1L
  private val internalSleepSand = BehaviorSubject.createDefault<Long>(NOT_ACTIVE)
  private var sleepDisposable: Disposable? = null
  private var shakeDisposable: Disposable? = null
  private var shakeTimeoutDisposable: Disposable? = null
  private val shakeObservable = shakeDetector.detect()

  init {
    // stops the player when the timer reaches 0
    internalSleepSand.filter { it == 0L } // when this reaches 0
      .subscribe {
        // stop the player
        playerController.stop()
      }

    internalSleepSand.subscribe {
      if (it > 0) {
        // enable shake timer
        resetTimerOnShake(true)
      } else if (it == NOT_ACTIVE) {
        // if the track ended by the user, disable the shake detector
        resetTimerOnShake(false)
      } else if (it == 0L) {
        // if the timer stopped normally, setup a timer of 5 minutes to resume playback
        resetTimerOnShake(true, 5)
      }
    }

    // counts down the sleep sand
    val sleepUpdateInterval = 1000L
    playStateManager.playState
      .map { it == PlayStateManager.PlayState.PLAYING }
      .distinctUntilChanged()
      .subscribe { playing ->
        if (playing) {
          sleepDisposable = Observable.interval(sleepUpdateInterval, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .filter { internalSleepSand.value > 0 } // only notify if there is still time left
            .map { internalSleepSand.value - sleepUpdateInterval } // calculate the new time
            .map { it.coerceAtLeast(0) } // but keep at least 0
            .subscribe { internalSleepSand.onNext(it) }
        } else {
          sleepDisposable?.dispose()
        }
      }
  }

  /** turns the sleep timer on or off **/
  fun setActive(enable: Boolean) {
    i { "toggleSleepSand. Left sleepTime is ${internalSleepSand.value}" }

    if (enable) {
      i { "Starting sleepTimer" }
      val minutes = prefsManager.sleepTime.value()
      internalSleepSand.onNext(TimeUnit.MINUTES.toMillis(minutes.toLong()))
    } else {
      i { "Cancelling sleepTimer" }
      internalSleepSand.onNext(NOT_ACTIVE)
    }
  }

  private fun resetTimerOnShake(enable: Boolean, stopAfter: Long? = null) {
    if (enable) {
      val shouldSubscribe = shakeDisposable?.isDisposed ?: true
      if (shouldSubscribe) {
        // setup shake detection if requested
        if (prefsManager.shakeToReset.value()) {
          shakeDisposable = shakeObservable.subscribe {
            if (internalSleepSand.value == 0L) {
              d { "detected shake while sleepSand==0. Resume playback" }
              playerController.play()
            }

            d { "reset now by shake" }
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
          d { "disabling pauseOnShake through timout" }
          resetTimerOnShake(false)
        }
    }
  }

  /**
   * This observable holds the time in ms left that the sleep timer has left. This is updated
   * periodically
   */
  val sleepSand: Observable<Long> = internalSleepSand

  fun sleepTimerActive(): Boolean = internalSleepSand.value > 0
}
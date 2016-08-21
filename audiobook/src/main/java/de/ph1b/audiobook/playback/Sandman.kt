package de.ph1b.audiobook.playback

import d
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import i
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.BehaviorSubject
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
    private val internalSleepSand = BehaviorSubject.create<Long>(-1L)
    private var sleepSubscription: Subscription? = null
    private var shakeSubscription: Subscription? = null
    private val shakeObservable = shakeDetector.create()

    init {
        // stops the player when the timer reaches 0
        internalSleepSand.filter { it == 0L } // when this reaches 0
                .subscribe {
                    // stop the player
                    playerController.stop()
                }

        // counts down the sleep sand
        val sleepUpdateInterval = 1000L
        playStateManager.playState
                .map { it == PlayStateManager.PlayState.PLAYING }
                .distinctUntilChanged()
                .subscribe { playing ->
                    if (playing) {
                        sleepSubscription = Observable.interval(sleepUpdateInterval, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                                .filter { internalSleepSand.value > 0 } // only notify if there is still time left
                                .map { internalSleepSand.value - sleepUpdateInterval } // calculate the new time
                                .map { it.coerceAtLeast(0) } // but keep at least 0
                                .subscribe { internalSleepSand.onNext(it) }
                    } else {
                        pauseOnShake(false)
                        sleepSubscription?.unsubscribe()
                    }
                }
    }

    /** turns the sleep timer on or off **/
    fun setActive(enable: Boolean) {
        i { "toggleSleepSand. Left sleepTime is ${internalSleepSand.value}" }

        pauseOnShake(enable)

        if (enable) {
            i { "Starting sleepTimer" }
            val minutes = prefsManager.sleepTime.value()
            internalSleepSand.onNext(TimeUnit.MINUTES.toMillis(minutes.toLong()))
        } else {
            i { "Cancelling sleepTimer" }
            internalSleepSand.onNext(-1L)
        }
    }

    private fun pauseOnShake(enabled: Boolean) {
        if (enabled) {
            val shouldSubscribe = shakeSubscription?.isUnsubscribed ?: true
            if (shouldSubscribe) {
                // setup shake detection if requested
                if (prefsManager.shakeToReset.value()) {
                    shakeSubscription = shakeObservable.subscribe {
                        d { "reset now by shake" }
                        setActive(true)
                    }
                }
            }
        } else shakeSubscription?.unsubscribe()
    }

    /**
     * This observable holds the time in ms left that the sleep timer has left. This is updated
     * periodically
     */
    val sleepSand: Observable<Long> = internalSleepSand.asObservable()

    fun sleepTimerActive(): Boolean = internalSleepSand.value > 0
}
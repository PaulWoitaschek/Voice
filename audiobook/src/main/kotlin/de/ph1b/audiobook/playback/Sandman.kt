/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.playback

import de.ph1b.audiobook.persistence.PrefsManager
import i
import rx.Observable
import rx.Subscription
import rx.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages everything sleep related.
 */
@Singleton class Sandman
@Inject constructor(playerController: PlayerController, playStateManager: PlayStateManager, private val prefsManager: PrefsManager) {

    /**
     * The time left till the playback stops in ms. If this is -1 the timer was stopped manually.
     * If this is 0 the timer simple counted down.
     */
    private val internalSleepSand = BehaviorSubject.create<Long>(-1L)
    private var sleepSubscription: Subscription? = null

    init {
        // stops the player when the timer reaches 0
        internalSleepSand.filter { it == 0L } // when this reaches 0
                .subscribe { playerController.stop() } // stop the player

        // counts down the sleep sand
        val sleepUpdateInterval = 1000L
        playStateManager.playState
                .map { it == PlayStateManager.PlayState.PLAYING }
                .distinctUntilChanged()
                .subscribe { playing ->
                    if (playing) {
                        sleepSubscription = Observable.interval(sleepUpdateInterval, TimeUnit.MILLISECONDS)
                                .filter { internalSleepSand.value > 0 } // only notify if there is still time left
                                .map { internalSleepSand.value - sleepUpdateInterval } // calculate the new time
                                .map { it.coerceAtLeast(0) } // but keep at least 0
                                .subscribe { internalSleepSand.onNext(it) }
                    } else {
                        sleepSubscription?.unsubscribe()
                    }
                }
    }

    /**
     * Turns the sleep timer on or off.
     *
     * @return true if the timer is now active, false if it now inactive
     */
    fun toggleSleepSand() {
        i { "toggleSleepSand. Left sleepTime is ${internalSleepSand.value}" }
        if (internalSleepSand.value > 0L) {
            i { "sleepSand is active. cancelling now" }
            internalSleepSand.onNext(-1L)
        } else {
            i { "preparing new sleep sand" }
            val minutes = prefsManager.sleepTime
            internalSleepSand.onNext(TimeUnit.MINUTES.toMillis(minutes.toLong()))
        }
    }


    /**
     * This observable holds the time in ms left that the sleep timer has left. This is updated
     * periodically
     */
    val sleepSand = internalSleepSand.asObservable()

    fun sleepTimerActive(): Boolean = internalSleepSand.value > 0
}
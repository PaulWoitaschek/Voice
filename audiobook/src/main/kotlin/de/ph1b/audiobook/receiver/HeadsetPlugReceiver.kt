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

package de.ph1b.audiobook.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import rx.Observable
import rx.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple receiver wrapper which holds a [BroadcastReceiver] that notifies on headset changes.
 *
 * @author Paul Woitaschek
 */
@Singleton
class HeadsetPlugReceiver
@Inject
constructor() {

    private val publishSubject = PublishSubject.create<HeadsetState>()

    fun observable(): Observable<HeadsetState> = publishSubject.asObservable()

    val broadcastReceiver = object : BroadcastReceiver() {
        private val PLUGGED = 1
        private val UNPLUGGED = 0

        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.i("onReceive with context=$context and intent=$intent")
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                val intState = intent?.getIntExtra("state", UNPLUGGED)
                if (intState == UNPLUGGED) {
                    publishSubject.onNext(HeadsetState.UNPLUGGED)
                } else if (intState == PLUGGED) {
                    publishSubject.onNext(HeadsetState.PLUGGED)
                } else {
                    Timber.i("Unknown headsetState $intState")
                }
            }
        }
    }

    enum class HeadsetState {
        PLUGGED,
        UNPLUGGED
    }
}
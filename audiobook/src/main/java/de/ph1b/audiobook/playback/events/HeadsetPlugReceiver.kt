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

package de.ph1b.audiobook.playback.events

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.ph1b.audiobook.misc.RxBroadcast
import i
import rx.Observable

/**
 * Simple receiver wrapper which holds a [android.content.BroadcastReceiver] that notifies on headset changes.
 *
 * @author Paul Woitaschek
 */
object HeadsetPlugReceiver {

    private val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
    private val PLUGGED = 1
    private val UNPLUGGED = 0

    fun events(c: Context): Observable<HeadsetState> = RxBroadcast.register(c, filter)
            .map {
                i { "onReceive with intent=$it" }
                val intState = it?.getIntExtra("state", UNPLUGGED)
                when (it?.getIntExtra("state", UNPLUGGED)) {
                    UNPLUGGED -> HeadsetState.UNPLUGGED
                    PLUGGED -> HeadsetState.PLUGGED
                    else -> {
                        i { "Unknown headsetState $intState" }
                        HeadsetState.UNKNOWN
                    }
                }
            }

    enum class HeadsetState {
        PLUGGED,
        UNPLUGGED,
        UNKNOWN
    }
}
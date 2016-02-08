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

import android.content.SharedPreferences
import android.os.Build
import de.ph1b.audiobook.persistence.edit
import de.ph1b.audiobook.persistence.setBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides information about if the custom media player can be used, or if there is a bug on the device.
 *
 * @author Paul Woitaschek
 */
class MediaPlayerCapabilities
@Inject
constructor(private val prefs: SharedPreferences, private val channelDetector: FalseChannelDetector) {

    fun useCustomMediaPlayer(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return false
        }

        // get key
        val contents = listOf<String?>(Build.VERSION.SDK_INT.toString(),
                Build.DEVICE,
                Build.MODEL,
                Build.PRODUCT,
                Build.MANUFACTURER,
                System.getProperty("os.version"),
                Build.BRAND)
        val builder = StringBuilder()
        contents.filter { it != null }
                .forEach { builder.append(it) }
        val key = builder.toString()

        if (prefs.contains(key)) {
            return prefs.getBoolean(key, false)
        } else {
            val canSetCustom = channelDetector.channelCountMatches()
            prefs.edit {
                setBoolean(key to canSetCustom)
            }
            return canSetCustom
        }
    }
}
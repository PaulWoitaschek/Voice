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

package de.ph1b.audiobook.logging

import java.text.SimpleDateFormat
import java.util.*
import java.util.Date as DateWithTime

/** Storage for logs. Stores up to [AMOUNT_OF_ENTRIES] logs for each day. After that removes the first */
object LogStorage {

    private const val AMOUNT_OF_ENTRIES = 1000

    private val logs = ArrayList<String>(AMOUNT_OF_ENTRIES)
    private val dateField = java.util.Date()
    private val format = SimpleDateFormat("HH:mm:ss")

    fun put(message: String) {
        // remove items if there are too many
        if (logs.size > AMOUNT_OF_ENTRIES) {
            logs.removeAt(0)
        }
        // add a timestamp
        dateField.time = System.currentTimeMillis()
        val stampMessage = "${format.format(dateField)}\t$message"
        logs.add(stampMessage)
    }

    fun get(): List<String> = logs
}
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

package de.ph1b.audiobook.persistence

import android.content.SharedPreferences
import org.json.JSONArray
import java.sql.Date
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** Storage for logs. Stores up to [AMOUNT_OF_ENTRIES] logs for each day. */
@Singleton
class LogStorage
@Inject
constructor(@Named(FOR) val storage: SharedPreferences) {

    private val logCache = HashMap<Date, MutableList<String>>()

    fun put(message: String) {

        val key = Date(System.currentTimeMillis())
        // get list from cache. If there is none, retrieve it from storage
        val cached = logCache[key]
        val listToUse: MutableList<String> = if (cached != null) {
            cached
        } else {
            val inStorage = storage.getString(key.toString(), null)
            val fromStorage: MutableList<String> = if (inStorage == null) {
                ArrayList()
            } else {
                JSONArray(inStorage).toMutableList()
            }
            logCache.put(key, fromStorage)
            fromStorage
        }

        // remove items if there are too many
        if (listToUse.size > AMOUNT_OF_ENTRIES) {
            listToUse.removeAt(0)
        }
        listToUse.add(message)

        // update storage
        storage.edit() {
            val content = JSONArray(listToUse)
            setString(key.toString() to content.toString())
        }
    }

    fun get(date: Date): List<String> {
        val key = date.toString()
        val value: String? = storage.getString(key, null)
        return if (value == null) emptyList() else JSONArray(value).toList()
    }

    companion object {
        const val FOR = "forLogStorage"
        private const val AMOUNT_OF_ENTRIES = 1000
    }
}
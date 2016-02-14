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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by ph1b on 14/02/16.
 */
@Singleton
class LogStorage
@Inject
constructor(@Named(FOR) val storage: SharedPreferences) {

    fun put(message: String) {
        val key = Date(System.currentTimeMillis()).toString()
        val value: String? = storage.getString(key, null)
        val content = if (value != null) JSONArray(value) else JSONArray()

        content.put(message)
        storage.edit() {
            setString(key to content.toString())
        }
    }

    fun get(date: Date): List<String> {
        val key = date.toString()
        val value: String? = storage.getString(key, null)
        return if (value == null) emptyList() else JSONArray(value).toList()
    }

    companion object {
        const val FOR = "forLogStorage"
    }
}
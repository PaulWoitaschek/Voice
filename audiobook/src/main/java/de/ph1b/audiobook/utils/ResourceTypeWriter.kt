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

package de.ph1b.audiobook.utils

import android.content.Context
import android.support.annotation.RawRes
import rx.Observable
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple helper for converting raw files to string output

 * @author Paul Woitaschek
 */
@Singleton class ResourceTypeWriter
@Inject constructor(private val context: Context) {

    fun rawToString(@RawRes rawRes: Int): Observable<String> {
        // defer so this can be run async
        return Observable.defer {
            val inputStream = context.resources.openRawResource(rawRes)
            val streamReader = InputStreamReader(inputStream, Charsets.UTF_8)
            // closes streamReader properly
            streamReader.use {
                Observable.just(it.readText())
            }
        }
    }
}
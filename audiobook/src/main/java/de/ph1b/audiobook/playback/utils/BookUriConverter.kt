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

package de.ph1b.audiobook.playback.utils

import android.content.UriMatcher
import android.net.Uri
import javax.inject.Inject

/**
 * Helper class for converting book and chapter ids to uris and back.
 *
 * @author Paul Woitaschek
 */
class BookUriConverter
@Inject constructor() {

    private fun baseBuilder() = Uri.Builder()
            .authority(booksAuthority)
            .appendPath(PATH_BOOKS)

    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(booksAuthority, PATH_BOOKS, ROOT)
        addURI(booksAuthority, "$PATH_BOOKS/#", BOOK_ID)
    }

    fun match(uri: Uri) = matcher.match(uri)

    fun allBooks(): Uri = baseBuilder().build()

    fun book(bookId: Long): Uri = baseBuilder()
            .appendPath(bookId.toString())
            .build()

    fun extractBook(uri: Uri) = uri.pathSegments[1].toLong()

    companion object {
        private const val booksAuthority = "BOOKS"
        private const val PATH_BOOKS = "root"
        const val ROOT = 1
        const val BOOK_ID = 2
    }
}
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

import android.content.UriMatcher
import android.net.Uri

/**
 * Helper class for converting book and chapter ids to uris and back.
 *
 * @author Paul Woitaschek
 */
object BookUriSpec {
    private const val booksAuthority = "BOOKS"
    private const val PATH_BOOKS = "books"
    private const val PATH_CHAPTERS = "chapters"
    const val BOOKS = 1
    const val BOOKS_ID = 2
    const val BOOK_CHAPTERS = 3
    const val BOOK_CHAPTERS_ID = 3

    private fun baseBuilder() = Uri.Builder()
            .authority(booksAuthority)
            .appendPath(PATH_BOOKS)

    val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(booksAuthority, PATH_BOOKS, BOOKS)
        addURI(booksAuthority, "$PATH_BOOKS/#", BOOKS_ID)
        addURI(booksAuthority, "$PATH_BOOKS/#/$PATH_CHAPTERS", BOOK_CHAPTERS)
        addURI(booksAuthority, "$PATH_BOOKS/#/$PATH_CHAPTERS/#", BOOK_CHAPTERS_ID)
    }

    fun allBooks() = baseBuilder().build()

    fun book(bookId: Long) = baseBuilder()
            .appendPath(bookId.toString())
            .build()

    fun bookChapters(bookId: Long) = baseBuilder()
            .appendPath(bookId.toString())
            .appendPath(PATH_CHAPTERS)
            .build()

    fun chapter(bookId: Long, chapterId: Long) = baseBuilder()
            .appendPath(bookId.toString())
            .appendPath(PATH_CHAPTERS)
            .appendPath(chapterId.toString())
            .build()

    fun extractBook(uri: Uri) = uri.pathSegments.get(1).toLong()

    fun extractChapter(uri: Uri) = uri.pathSegments.get(3).toLong()
}
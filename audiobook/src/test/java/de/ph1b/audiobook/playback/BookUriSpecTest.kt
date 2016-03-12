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

import android.os.Build
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.TestApp
import org.fest.assertions.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Test for the book uri spec

 * @author Paul Woitaschek
 */
@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), manifest = "src/main/AndroidManifest.xml", application = TestApp::class)
class BookUriSpecTest {

    init {
        ShadowLog.stream = System.out
    }

    @Test
    fun testAllBooks() {
        val uri = BookUriSpec.allBooks()

        println(uri)

        val match = BookUriSpec.matcher.match(uri)

        assertThat(match).isEqualTo(BookUriSpec.BOOKS)
    }

    @Test
    fun testSingleBook() {
        val uri = BookUriSpec.book(5)

        val match = BookUriSpec.matcher.match(uri)

        assertThat(match).isEqualTo(BookUriSpec.BOOKS_ID)
    }

    @Test
    fun testSingleChapter() {
        val uri = BookUriSpec.chapter(5, 6)

        val match = BookUriSpec.matcher.match(uri)

        assertThat(match).isEqualTo(BookUriSpec.BOOK_CHAPTERS_ID)
    }

    @Test
    fun testChapters() {
        val uri = BookUriSpec.bookChapters(5)

        val match = BookUriSpec.matcher.match(uri)

        assertThat(match).isEqualTo(BookUriSpec.BOOK_CHAPTERS)
    }

    @Test
    fun testExtractBookAndChapterId() {
        val bookId = 7L
        val chapterId = 42L

        val uri = BookUriSpec.chapter(bookId, chapterId)

        val extractedBookId = BookUriSpec.extractBook(uri)
        val extractedChapterId = BookUriSpec.extractChapter(uri)

        assertThat(bookId).isEqualTo(extractedBookId)
        assertThat(chapterId).isEqualTo(extractedChapterId)
    }

    @Test
    fun testExtractBookOnly() {
        val bookId = 153L

        val uri = BookUriSpec.book(bookId)

        val extracted = BookUriSpec.extractBook(uri)

        assertThat(bookId).isEqualTo(extracted)
    }
}
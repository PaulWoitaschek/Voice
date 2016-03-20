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
import org.junit.Before
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
class BookUriConverterTest {

    lateinit var converter: BookUriConverter

    init {
        ShadowLog.stream = System.out
    }

    @Before
    fun setUp() {
        converter = BookUriConverter()
    }

    @Test
    fun testAllBooks() {
        val uri = converter.allBooks()

        println(uri)

        val match = converter.match(uri)

        assertThat(match).isEqualTo(BookUriConverter.ROOT)
    }

    @Test
    fun testSingleBook() {
        val uri = converter.book(1)

        val match = converter.match(uri)

        assertThat(match).isEqualTo(BookUriConverter.BOOK_ID)
    }

    @Test
    fun testExtractBookOnly() {
        val bookId = 153L

        val uri = converter.book(bookId)

        val extracted = converter.extractBook(uri)

        assertThat(bookId).isEqualTo(extracted)
    }
}
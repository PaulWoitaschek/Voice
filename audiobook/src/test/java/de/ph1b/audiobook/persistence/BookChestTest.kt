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

import android.os.Build
import de.ph1b.audiobook.BookMocker
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.TestApp
import de.ph1b.audiobook.persistence.internals.InternalBookRegister
import de.ph1b.audiobook.persistence.internals.InternalDb
import org.fest.assertions.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Test for the book chest.
 *
 * @author: Paul Woitaschek
 */
@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), manifest = "src/main/AndroidManifest.xml", application = TestApp::class)
class BookChestTest {

    init {
        ShadowLog.stream = System.out
    }

    private lateinit var bookChest: BookChest

    @Before
    fun setUp() {
        val internalDb = InternalDb(RuntimeEnvironment.application)
        val internalBookRegister = InternalBookRegister(internalDb)
        bookChest = BookChest(internalBookRegister)
    }

    @Test
    fun testBookChest() {
        var dummy = BookMocker.mock(5)
        bookChest.addBook(dummy)
        val firstBook = bookChest.activeBooks.first()
        var dummyWithUpdatedId = dummy.copy(id = firstBook.id)

        assertThat(dummyWithUpdatedId).isEqualTo(firstBook)
    }

    /**
     * Test that makes sure that adding a book with the same root and type throws an exception.
     */
    @Test
    fun testThatDoubleInsertThrows() {
        val dummy = BookMocker.mock(-1)
        bookChest.addBook(dummy)

        val bookWithSameContent = dummy.copy(id = -1)

        var thrown = false
        try {
            bookChest.addBook(bookWithSameContent)
        } catch(e: Exception) {
            e.printStackTrace()
            thrown = true
        }

        assertThat(thrown).isTrue
        assertThat(bookChest.activeBooks).hasSize(1)
    }
}
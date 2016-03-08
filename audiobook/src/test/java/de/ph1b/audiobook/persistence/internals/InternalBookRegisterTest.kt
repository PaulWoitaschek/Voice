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

package de.ph1b.audiobook.persistence.internals

import android.os.Build
import de.ph1b.audiobook.BookMocker
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.TestApp
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.model.Chapter
import org.fest.assertions.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.io.File
import java.util.*


/**
 * Simple test for book persistence.
 *
 * @author Paul Woitaschek
 */
@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), manifest = "src/main/AndroidManifest.xml", application = TestApp::class)
class InternalBookRegisterTest {

    init {
        ShadowLog.stream = System.out
    }

    lateinit var register: InternalBookRegister

    @Before
    fun setUp() {
        val internalDb = InternalDb(RuntimeEnvironment.application, "db")
        register = InternalBookRegister(internalDb)
    }

    @Test
    fun testHideRevealBook() {
        val mock = BookMocker.mock(-1)
        register.addBook(mock)

        val activeBooks = register.activeBooks()
        val inactiveBooks = register.orphanedBooks()

        assertThat(activeBooks).hasSize(1)
        assertThat(inactiveBooks).isEmpty()

        register.hideBook(activeBooks.first().id)

        assertThat(register.activeBooks()).isEmpty()
        assertThat(register.orphanedBooks()).hasSize(1)

        val hiddenBook = register.orphanedBooks().single()

        register.revealBook(hiddenBook.id)

        assertThat(register.activeBooks()).hasSize(1)
        assertThat(register.orphanedBooks()).isEmpty()
    }

    /**
     * Tests that the returned book matches the retrieved one
     */
    @Test
    fun testAddBookReturn() {
        val mock = BookMocker.mock(-1)
        val inserted = register.addBook(mock)
        val retrieved = register.activeBooks().single()

        assertThat(inserted).isEqualTo(retrieved)
    }

    @Test
    fun testAddBook() {
        val mock1 = BookMocker.mock(-1)
        val mock2 = BookMocker.mock(-1)
        val firstInserted = register.addBook(mock1)
        val secondInserted = register.addBook(mock2)

        val containing = register.activeBooks()

        assertThat(containing).hasSize(2)

        val mock1WithUpdatedId = mock1.copy(id = firstInserted.id)
        val mock2WithUpdatedId = mock2.copy(id = secondInserted.id)

        assertThat(containing).doesNotContain(mock1, mock2)
        assertThat(containing).contains(mock1WithUpdatedId, mock2WithUpdatedId)
    }

    @Test
    fun testAddBookWithNullableAuthor() {
        val mock = BookMocker.mock(-1)
        val withNullableAuthor = mock.copy(author = null)
        val inserted = register.addBook(withNullableAuthor)

        assertThat(inserted)
                .isEqualTo(withNullableAuthor.copy(id = inserted.id))

        val retrieved = register.activeBooks()
                .single()

        assertThat(retrieved).isEqualTo(withNullableAuthor.copy(id = retrieved.id))
    }


    @Test
    fun testUpdateBook() {
        val mock = BookMocker.mock(-1)
        val inserted = register.addBook(mock)

        val oldChapters = inserted.chapters
        val substracted = oldChapters.minus(oldChapters.first())
        val newChapters = substracted.plus(Chapter(File("/root/", "salkjsdg.mp3"), "askhsdglkjsdf", 113513516))

        val changed = inserted.copy(
                type = Book.Type.SINGLE_FILE,
                useCoverReplacement = true,
                author = if (Random().nextBoolean()) "lkajsdflkj" else null,
                currentFile = newChapters.last().file,
                time = 135135135,
                name = "252587245",
                chapters = newChapters,
                playbackSpeed = 1.7f,
                root = "slksjdglkjgaölskjdf")

        register.updateBook(changed)

        val containingBooks = register.activeBooks()

        // check that there is still only one book
        assertThat(containingBooks).hasSize(1)

        assertThat(containingBooks).doesNotContain(inserted)
        assertThat(containingBooks).contains(changed)
    }
}
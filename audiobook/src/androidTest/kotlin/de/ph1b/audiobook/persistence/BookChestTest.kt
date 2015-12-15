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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.persistence

import android.test.AndroidTestCase
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.testing.DummyCreator
import java.io.File
import javax.inject.Inject

/**
 * Tests the book storage.
 *
 * @author Paul Woitaschek
 */
class BookChestTest : AndroidTestCase() {

    @Inject lateinit var bookChest: BookChest


    /**
     * Tests if the book we insert inTestsTTto our bookshelf is the same we retrieve later
     */
    fun testBookInOut() {
        App.component().inject(this)

        // add a fresh book to the database
        val bookIn = DummyCreator.dummyBook(File("/storage/one.mp3"), File("/storage/two.mp3"))
        bookChest.addBook(bookIn)

        // recall inject so we have a new instance of book-shelf
        (context.applicationContext as App).initNewComponent()
        App.component().inject(this)

        // retrieve the book by chapters
        val allBooks = bookChest.activeBooks.toList().toBlocking().first()
        var retrievedBook: Book? = null
        for (book in allBooks) {
            if (book.chapters == bookIn.chapters) {
                retrievedBook = book
            }
        }

        // make sure a book was retrieved
        checkNotNull(retrievedBook)

        // set ids the same so equals can be used to compare the input book with the output book
        val addedBookWithUpdatedId = bookIn.copy(id = retrievedBook!!.id)
        check(addedBookWithUpdatedId == retrievedBook)
    }
}
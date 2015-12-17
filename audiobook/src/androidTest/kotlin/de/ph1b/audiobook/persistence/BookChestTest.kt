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

import android.test.ApplicationTestCase
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.model.BookAdder
import de.ph1b.audiobook.testing.DummyCreator
import de.ph1b.audiobook.testing.RealFileMocker
import java.util.*
import javax.inject.Inject

/**
 * Tests the book storage.
 *
 * @author Paul Woitaschek
 */
class BookChestTest : ApplicationTestCase<App> (App::class.java) {

    override fun setUp() {
        super.setUp()

        createApplication()
        App.component().inject(this)
        realFileMocker.create(context)
    }

    @Inject lateinit var bookChest: BookChest
    @Inject lateinit var bookAdder: BookAdder
    @Inject lateinit var prefsManager: PrefsManager

    private val realFileMocker = RealFileMocker()


    /**
     * Tests if the book we insert inTestsTTto our bookshelf is the same we retrieve later
     */
    fun testBookInOut() {
        val bookIn = DummyCreator.dummyBook(realFileMocker.file1, realFileMocker.file2)
        val singleFolders = ArrayList(prefsManager.singleBookFolders)
        singleFolders.add(bookIn.currentFile.parentFile.absolutePath)
        prefsManager.singleBookFolders = singleFolders
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

        // check if the current file exists
        check(addedBookWithUpdatedId.currentFile.exists())

        // delete the current file and check that it does not exist any longer
        addedBookWithUpdatedId.currentFile.delete()
        check(addedBookWithUpdatedId.currentFile.exists().not())

        // start the book adder
        bookAdder.scanForFiles(true)
        Thread.sleep(100)
        check(bookAdder.scannerActive().value)

        // now we wait for the scanner to complete
        val newActiveState = bookAdder.scannerActive().filter({ it.not() })
                .toBlocking()
                .first()
        check(newActiveState.not())

        // as the we deleted the file now the book adder should have removed the chapter.
        val bookRetrievedAgain = bookChest.activeBooks.toBlocking().single { it.id == addedBookWithUpdatedId.id }
        check(bookRetrievedAgain.currentChapter() != addedBookWithUpdatedId.currentChapter())
    }

    override fun tearDown() {
        realFileMocker.destroy()
        prefsManager.singleBookFolders = ArrayList()

        super.tearDown()
    }
}
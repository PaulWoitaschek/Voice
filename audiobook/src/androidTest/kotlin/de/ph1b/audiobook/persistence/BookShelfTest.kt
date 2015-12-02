package de.ph1b.audiobook.persistence

import android.test.AndroidTestCase
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.testing.MockProvider
import java.io.File
import javax.inject.Inject

/**
 * Tests the book storage.
 *
 * @author Paul Woitaschek
 */
class BookShelfTest : AndroidTestCase() {

    @Inject lateinit var bookShelf: BookShelf


    /**
     * Tests if the book we insert inTestsTTto our bookshelf is the same we retrieve later
     */
    fun testBookInOut() {
        val provider = MockProvider(context)

        provider.newMockComponent().inject(this)

        // add a fresh book to the database
        val bookIn = provider.dummyBook(File("/storage/one.mp3"), File("/storage/two.mp3"))
        bookShelf.addBook(bookIn)

        // recall inject so we have a new instance of book-shelf
        provider.newMockComponent().inject(this)

        // retrieve the book by chapters
        val allBooks = bookShelf.activeBooks.toList().toBlocking().first()
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
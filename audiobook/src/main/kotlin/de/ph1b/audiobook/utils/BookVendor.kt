package de.ph1b.audiobook.utils

import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookShelf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple helper that provides blocking access to [BookShelf]

 * @author Paul Woitaschek
 */
@Singleton
class BookVendor
@Inject
constructor(private val bookShelf: BookShelf) {

    fun byId(id: Long): Book? {
        return bookShelf.activeBooks
                .toBlocking()
                .singleOrDefault(null, { it.id == id })
    }

    fun all(): List<Book> {
        return bookShelf.activeBooks.toList().toBlocking().first()
    }
}

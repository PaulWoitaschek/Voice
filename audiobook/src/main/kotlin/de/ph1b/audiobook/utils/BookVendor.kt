package de.ph1b.audiobook.utils

import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookShelf
import rx.functions.Func1
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
        return bookShelf.getActiveBooks()
                .singleOrDefault(null, Func1 { it.id == id })
                .toBlocking()
                .single()
    }

    fun all(): List<Book> {
        return bookShelf.getActiveBooks().toList().toBlocking().first()
    }
}

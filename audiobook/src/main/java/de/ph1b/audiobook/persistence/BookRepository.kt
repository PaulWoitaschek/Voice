package de.ph1b.audiobook.persistence

import de.ph1b.audiobook.Book
import de.ph1b.audiobook.persistence.internals.BookStorage
import e
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import v
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Provides access to all books.
 *
 * @author Paul Woitaschek
 */
@Singleton class BookRepository
@Inject constructor(private val storage: BookStorage) {

    private val active: MutableList<Book> by lazy { storage.activeBooks().toMutableList() }
    private val orphaned: MutableList<Book> by lazy { storage.orphanedBooks().toMutableList() }

    private val updated = PublishSubject.create<Book>()

    private val all: BehaviorSubject<List<Book>> by lazy { BehaviorSubject.createDefault<List<Book>>(active) }

    fun updateObservable(): Observable<Book> = updated

    fun booksStream(): Observable<List<Book>> = all

    private fun sortBooksAndNotifySubject() {
        active.sort()
        all.onNext(active)
    }

    @Synchronized fun addBook(book: Book) {
        v { "addBook=${book.name}" }

        val bookWithId = storage.addBook(book)
        active.add(bookWithId)
        sortBooksAndNotifySubject()
    }

    /** All active books. */
    val activeBooks: List<Book>
        get() = synchronized(this) { ArrayList(active) }

    @Synchronized fun bookById(id: Long) = active.firstOrNull { it.id == id }

    @Synchronized fun getOrphanedBooks(): List<Book> = ArrayList(orphaned)

    @Synchronized fun updateBook(book: Book, chaptersChanged: Boolean = false) {
        v { "updateBook=${book.name} with time ${book.time}" }

        val index = active.indexOfFirst { it.id == book.id }
        if (index != -1) {
            active[index] = book
            storage.updateBook(book, chaptersChanged)
            updated.onNext(book)
            sortBooksAndNotifySubject()
        } else e { "update failed as there was no book" }
    }

    @Synchronized fun hideBook(toDelete: List<Book>) {
        v { "hideBooks=${toDelete.size}" }

        val idsToDelete = toDelete.map(Book::id)
        active.removeAll { idsToDelete.contains(it.id) }
        orphaned.addAll(toDelete)
        sortBooksAndNotifySubject()
    }


    @Synchronized fun revealBook(book: Book) {
        v { "Called revealBook=$book" }

        orphaned.removeAll { it.id == book.id }
        storage.revealBook(book.id)
        active.add(book)
        sortBooksAndNotifySubject()
    }
}

package de.ph1b.audiobook.persistence

import de.ph1b.audiobook.Book
import de.ph1b.audiobook.persistence.internals.InternalBookRegister
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import v
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Provides access to all books.

 * @author Paul Woitaschek
 */
@Singleton class BookChest
@Inject constructor(private val register: InternalBookRegister) {

    private val active: MutableList<Book> by lazy { register.activeBooks().toMutableList() }
    private val orphaned: MutableList<Book> by lazy { register.orphanedBooks().toMutableList() }

    private val updated = PublishSubject.create<Book>()

    private val all: BehaviorSubject<List<Book>> by lazy { BehaviorSubject.create<List<Book>>(active) }

    fun updateObservable(): Observable<Book> = updated.asObservable()

    fun booksStream(): Observable<List<Book>> = all.asObservable()

    private fun sortBooksAndNotifySubject() {
        active.sort()
        all.onNext(active)
    }

    @Synchronized fun addBook(book: Book) {
        v { "addBook=${book.name}" }

        val bookWithId = register.addBook(book)
        active.add(bookWithId)
        sortBooksAndNotifySubject()
    }

    /**
     * All active books. We
     */
    val activeBooks: List<Book>
        get() = synchronized(active) { ArrayList(active) }

    fun bookById(id: Long) = active.firstOrNull { it.id == id }

    @Synchronized fun getOrphanedBooks(): List<Book> = ArrayList(orphaned)

    @Synchronized fun updateBook(book: Book, chaptersChanged: Boolean = false) {
        v { "updateBook=${book.name} with time ${book.time}" }

        val bookIterator = active.listIterator()
        while (bookIterator.hasNext()) {
            val next = bookIterator.next()
            if (book.id == next.id) {
                bookIterator.set(book)
                register.updateBook(book, chaptersChanged)
                break
            }
        }

        updated.onNext(book)
        sortBooksAndNotifySubject()
    }

    @Synchronized fun hideBook(book: Book) {
        v { "hideBook=${book.name}" }

        val iterator = active.listIterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.id == book.id) {
                iterator.remove()
                register.hideBook(book.id)
                break
            }
        }
        orphaned.add(book)
        sortBooksAndNotifySubject()
    }

    @Synchronized fun revealBook(book: Book) {
        v { "Called revealBook=$book" }

        val orphanedBookIterator = orphaned.iterator()
        while (orphanedBookIterator.hasNext()) {
            if (orphanedBookIterator.next().id == book.id) {
                orphanedBookIterator.remove()
                register.revealBook(book.id)
                break
            }
        }
        active.add(book)
        sortBooksAndNotifySubject()
    }
}

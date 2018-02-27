package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.internals.BookStorage
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides access to all books.
 */
@Singleton
class BookRepository
@Inject constructor(private val storage: BookStorage) {

  private val active: MutableList<Book> by lazy {
    storage.activeBooks().toMutableList().apply { sort() }
  }
  private val orphaned: MutableList<Book> by lazy { storage.orphanedBooks().toMutableList() }

  private val updated = PublishSubject.create<Book>()

  private val all: BehaviorSubject<List<Book>> by lazy {
    BehaviorSubject.createDefault<List<Book>>(
      active
    )
  }

  fun updateObservable(): Observable<Book> = updated

  fun booksStream(): Observable<List<Book>> = all

  private fun sortBooksAndNotifySubject() {
    active.sort()
    all.onNext(active)
  }

  @Synchronized
  fun addBook(book: Book) {
    Timber.v("addBook=${book.name}")

    val bookWithId = storage.addBook(book)
    active.add(bookWithId)
    sortBooksAndNotifySubject()
  }

  /** All active books. */
  val activeBooks: List<Book>
    get() = synchronized(this) { ArrayList(active) }

  @Synchronized
  fun bookById(id: Long) = active.firstOrNull { it.id == id }

  @Synchronized
  fun getOrphanedBooks(): List<Book> = ArrayList(orphaned)

  @Synchronized
  fun updateBook(book: Book) {
    require(book.id != -1L)

    val index = active.indexOfFirst { it.id == book.id }
    if (index != -1) {
      active[index] = book
      storage.updateBook(book)
      updated.onNext(book)
      sortBooksAndNotifySubject()
    } else Timber.e("update failed as there was no book")
  }

  @Synchronized
  fun hideBook(toDelete: List<Book>) {
    Timber.v("hideBooks=${toDelete.size}")
    if (toDelete.isEmpty()) return

    val idsToDelete = toDelete.map(Book::id)
    active.removeAll { idsToDelete.contains(it.id) }
    orphaned.addAll(toDelete)
    toDelete.forEach { storage.hideBook(it.id) }
    sortBooksAndNotifySubject()
  }

  @Synchronized
  fun revealBook(book: Book) {
    Timber.v("Called revealBook=$book")

    orphaned.removeAll { it.id == book.id }
    active.add(book)
    storage.revealBook(book.id)
    sortBooksAndNotifySubject()
  }

  @Synchronized
  fun chapterByFile(file: File) = chapterByFile(file, active) ?: chapterByFile(file, orphaned)

  private fun chapterByFile(file: File, books: List<Book>): Chapter? {
    books.forEach {
      it.chapters.forEach {
        if (it.file == file) return it
      }
    }
    return null
  }
}

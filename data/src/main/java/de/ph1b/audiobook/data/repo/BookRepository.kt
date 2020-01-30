package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.common.Optional
import de.ph1b.audiobook.common.orNull
import de.ph1b.audiobook.common.toOptional
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.internals.BookStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.ArrayList
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository
@Inject constructor(private val storage: BookStorage) {

  private val allBooks by lazy {
    runBlocking { storage.books() }
  }
  private val active: MutableList<Book> by lazy {
    val activeBooks = allBooks.filter { it.content.settings.active }
    Collections.synchronizedList(activeBooks)
  }
  private val orphaned: MutableList<Book> by lazy {
    val orphanedBooks = allBooks.filter { it.content.settings.active }
    Collections.synchronizedList(orphanedBooks)
  }

  private val activeBooksSubject: BehaviorSubject<List<Book>> by lazy {
    BehaviorSubject.createDefault<List<Book>>(active)
  }

  fun booksStream(): Observable<List<Book>> = activeBooksSubject

  fun byId(id: UUID): Observable<Optional<Book>> {
    return activeBooksSubject.map { books ->
      books.find { it.id == id }.toOptional()
    }
  }

  suspend fun addBook(book: Book) {
    withContext(Dispatchers.IO) {
      Timber.v("addBook=${book.name}")

      storage.addOrUpdate(book)
      active.add(book)
      withContext(Dispatchers.Main) {
        activeBooksSubject.onNext(active.toList())
      }
    }
  }

  /** All active books. */
  val activeBooks: List<Book>
    get() = synchronized(this) { ArrayList(active) }

  fun bookById(id: UUID) = active.firstOrNull { it.id == id }

  suspend fun updateBookContent(content: BookContent) {
    updateBookInMemory(content.id) {
      updateContent { content }
    }
    storage.updateBookContent(content)
  }

  fun getOrphanedBooks(): List<Book> = ArrayList(orphaned)

  suspend fun updateBook(book: Book) {
    if (bookById(book.id) == book) {
      return
    }
    withContext(Dispatchers.IO) {
      val index = active.indexOfFirst { it.id == book.id }
      if (index != -1) {
        active[index] = book
        storage.addOrUpdate(book)
        withContext(Dispatchers.Main) {
          activeBooksSubject.onNext(active.toList())
        }
      } else Timber.e("update failed as there was no book")
    }
  }

  suspend fun updateBookName(id: UUID, name: String) {
    withContext(Dispatchers.IO) {
      storage.updateBookName(id, name)
      updateBookInMemory(id) {
        updateMetaData { copy(name = name) }
      }
    }
  }

  private suspend inline fun updateBookInMemory(id: UUID, update: Book.() -> Book) {
    val index = active.indexOfFirst { it.id == id }
    if (index != -1) {
      active[index] = update(active[index])
      withContext(Dispatchers.Main) {
        activeBooksSubject.onNext(active.toList())
      }
    } else {
      Timber.e("update failed as there was no book")
    }
  }

  suspend fun markBookAsPlayedNow(id: UUID) {
    withContext(Dispatchers.IO) {
      val lastPlayedAt = System.currentTimeMillis()
      storage.updateLastPlayedAt(id, lastPlayedAt)
      updateBookInMemory(id) {
        update(updateSettings = {
          copy(lastPlayedAtMillis = lastPlayedAt)
        })
      }
    }
  }

  suspend fun hideBook(toDelete: List<Book>) {
    withContext(Dispatchers.IO) {
      Timber.v("hideBooks=${toDelete.size}")
      if (toDelete.isEmpty()) return@withContext

      val idsToDelete = toDelete.map(Book::id)
      active.removeAll { idsToDelete.contains(it.id) }
      orphaned.addAll(toDelete)
      toDelete.forEach { storage.hideBook(it.id) }
      withContext(Dispatchers.Main) {
        activeBooksSubject.onNext(active.toList())
      }
    }
  }

  suspend fun revealBook(book: Book) {
    withContext(Dispatchers.IO) {
      Timber.v("Called revealBook=$book")

      orphaned.removeAll { it.id == book.id }
      active.add(book)
      storage.revealBook(book.id)
      withContext(Dispatchers.Main) {
        activeBooksSubject.onNext(active.toList())
      }
    }
  }

  fun chapterByFile(file: File) = chapterByFile(file, active) ?: chapterByFile(file, orphaned)

  private fun chapterByFile(file: File, books: List<Book>): Chapter? {
    books.forEach { book ->
      book.content.chapters.forEach { chapter ->
        if (chapter.file == file) return chapter
      }
    }
    return null
  }
}

fun BookRepository.flowById(bookId: UUID): Flow<Book?> {
  return byId(bookId)
    .toFlowable(BackpressureStrategy.LATEST)
    .asFlow()
    .map { it.orNull }
}

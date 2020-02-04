package de.ph1b.audiobook.data.repo.internals

import android.os.Looper
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class MemoryRepo
@Inject constructor(
  private val storage: BookStorage
) {

  private val listLock = Mutex()

  private suspend inline fun <T> locked(action: () -> T): T {
    return listLock.withLock(action = action)
  }

  private val allBooks by lazy {
    runBlocking { storage.books() }
  }
  private val active: MutableList<Book> by lazy {
    allBooks.filter { it.content.settings.active }.toMutableList()
  }
  private val orphaned: MutableList<Book> by lazy {
    allBooks.filter { !it.content.settings.active }.toMutableList()
  }

  private val activeBooksSubject: BehaviorSubject<List<Book>> by lazy {
    BehaviorSubject.createDefault<List<Book>>(active)
  }

  fun stream(): Observable<List<Book>> = activeBooksSubject

  suspend fun active(): List<Book> = locked {
    active.toList()
  }

  suspend fun replace(book: Book): Boolean = locked {
    val index = active.indexOfFirst { it.id == book.id }
    if (index == -1) {
      false
    } else {
      active[index] = book
      updateActiveBookSubject()
      true
    }
  }

  suspend fun orphaned(): List<Book> = locked {
    orphaned.toList()
  }

  suspend fun hide(toDelete: List<Book>) = locked {
    val idsToDelete = toDelete.map(Book::id)
    val somethingRemoved = active.removeAll { it.id in idsToDelete }
    orphaned.addAll(toDelete)
    if (somethingRemoved) {
      updateActiveBookSubject()
    }
  }

  suspend fun reveal(book: Book) = locked {
    orphaned.removeAll { it.id == book.id }
    active.add(book)
    updateActiveBookSubject()
  }

  suspend fun add(book: Book) {
    locked {
      active.add(book)
      updateActiveBookSubject()
    }
  }

  suspend fun chapterByFile(file: File): Chapter? = locked {
    active.chapterByFile(file) ?: orphaned.chapterByFile(file)
  }

  private suspend fun updateActiveBookSubject() {
    if (Looper.getMainLooper() == Looper.myLooper()) {
      activeBooksSubject.onNext(active.toList())
    } else {
      withContext(Dispatchers.Main) {
        activeBooksSubject.onNext(active.toList())
      }
    }
  }
}

private fun List<Book>.chapterByFile(file: File): Chapter? {
  forEach { book ->
    book.content.chapters.forEach { chapter ->
      if (chapter.file == file) return chapter
    }
  }
  return null
}

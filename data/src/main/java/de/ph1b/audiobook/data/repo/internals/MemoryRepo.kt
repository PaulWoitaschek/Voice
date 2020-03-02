package de.ph1b.audiobook.data.repo.internals

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

  private val activeBooksSubject = ConflatedBroadcastChannel<List<Book>>(active)
  val activeBooks: Flow<List<Book>> get() = activeBooksSubject.asFlow()

  suspend fun active(): List<Book> = locked {
    active.toList()
  }

  suspend fun replace(book: Book): Boolean = locked {
    val index = active.indexOfFirst { it.id == book.id }
    if (index == -1) {
      false
    } else {
      if (active[index] == book) {
        false
      } else {
        active[index] = book
        updateActiveBookSubject()
        true
      }
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
    activeBooksSubject.send(active.toList())
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

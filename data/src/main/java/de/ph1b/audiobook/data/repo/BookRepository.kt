package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.internals.BookStorage
import de.ph1b.audiobook.data.repo.internals.MemoryRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository
@Inject constructor(
  private val storage: BookStorage
) {

  private val memory = MemoryRepo(
    runBlocking {
      storage.books()
    }
  )

  fun flow(): Flow<List<Book>> {
    return memory.flow.map { books ->
      books.filter { it.content.settings.active }
    }
  }

  fun flow(bookId: UUID): Flow<Book?> {
    return memory.flow.map { books -> books.find { it.id == bookId } }
  }

  fun bookById(id: UUID): Book? = memory.allBooks().find { it.id == id }

  fun allBooks(): List<Book> = memory.allBooks()

  fun activeBooks(): List<Book> = memory.allBooks().filterActive(true)

  suspend fun addBook(book: Book) {
    Timber.v("addBook=${book.name}")
    require(book.content.settings.active) {
      "Book $book must be active"
    }
    memory.addOrUpdate(book)
    storage.addOrUpdate(book)
  }

  suspend fun updateBookContent(content: BookContent): Book? {
    val updated = memory.updateBookContent(content)
    storage.updateBookContent(content)
    return updated
  }

  suspend fun updateBookName(id: UUID, name: String) {
    memory.updateBookName(id, name)
    storage.updateBookName(id, name)
  }

  suspend fun setBookActive(bookId: UUID, active: Boolean) {
    Timber.d("setBookActive(bookId=$bookId, active=$active)")
    memory.setBookActive(bookId, active)
    storage.setBookActive(bookId, active)
  }

  fun chapterByFile(file: File): Chapter? {
    memory.allBooks().forEach { book ->
      val chapter = book.content.chapters.find { it.file == file }
      if (chapter != null) {
        return chapter
      }
    }
    return null
  }
}

private fun List<Book>.filterActive(active: Boolean): List<Book> {
  return filter { it.content.settings.active == active }
}

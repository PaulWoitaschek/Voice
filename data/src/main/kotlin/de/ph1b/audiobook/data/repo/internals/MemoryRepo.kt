package de.ph1b.audiobook.data.repo.internals

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import javax.inject.Inject

class MemoryRepo
@Inject constructor(
  initial: List<Book>
) {

  private val books = MutableStateFlow(initial)
  val flow: Flow<List<Book>> = books

  fun addOrUpdate(book: Book) {
    val updated = books.value.toMutableList().apply {
      removeAll { it.id == book.id }
      add(book)
    }
    books.value = updated
  }

  fun updateBookContent(content: BookContent): Book? {
    return updateBook(content.id) {
      copy(content = content)
    }
  }

  fun updateBookName(id: UUID, name: String) {
    updateBook(id) {
      update(
        updateMetaData = {
          copy(name = name)
        }
      )
    }
  }

  fun setBookActive(bookId: UUID, active: Boolean) {
    updateBook(bookId) {
      update(updateSettings = { copy(active = active) })
    }
  }

  private fun updateBook(id: UUID, update: Book.() -> Book): Book? {
    val books = books.value
    val index = books.indexOfFirst { it.id == id }
    if (index == -1) {
      return null
    }
    val currentBook = books[index]
    val updated = update(currentBook)
    val updatedBooks = books.toMutableList().apply {
      set(index, updated)
    }
    this.books.value = updatedBooks
    return updated
  }

  fun allBooks(): List<Book> = books.value
}

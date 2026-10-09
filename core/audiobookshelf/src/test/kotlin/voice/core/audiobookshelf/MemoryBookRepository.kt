package voice.core.audiobookshelf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.repo.BookRepository

class MemoryBookRepository(vararg books: Book) : BookRepository {

  private val books = MutableStateFlow(books.toList())

  override fun flow(): Flow<List<Book>> = books

  override suspend fun all(): List<Book> = books.value

  override fun flow(id: BookId): Flow<Book?> = books.map { books -> books.find { it.id == id } }

  override suspend fun get(id: BookId): Book? = books.value.find { it.id == id }

  override suspend fun updateBook(
    id: BookId,
    update: (BookContent) -> BookContent,
  ) {
    books.update { books ->
      books.map { if (it.id == id) it.update(update) else it }
    }
  }
}

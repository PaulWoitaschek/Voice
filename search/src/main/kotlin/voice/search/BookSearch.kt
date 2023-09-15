package voice.search

import voice.data.Book
import voice.data.repo.BookRepository
import voice.data.repo.internals.dao.BookContentDao
import javax.inject.Inject

class BookSearch
@Inject constructor(
  private val dao: BookContentDao,
  private val repo: BookRepository,
) {

  suspend fun search(query: String): List<Book> {
    return dao.search(
      buildString {
        append("\"")
        append('*')
        append(query.trim().replace("\"", "\"\""))
        append('*')
        append("\"")
      },
    ).mapNotNull { repo.get(it) }
  }
}

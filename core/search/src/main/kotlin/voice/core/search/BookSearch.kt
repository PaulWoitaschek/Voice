package voice.core.search

import dev.zacsweers.metro.Inject
import voice.core.data.Book
import voice.core.data.repo.BookRepository
import voice.core.data.repo.internals.dao.BookContentDao

@Inject
class BookSearch(
  private val dao: BookContentDao,
  private val repo: BookRepository,
) {

  suspend fun search(query: String): List<Book> {
    val searchString = buildString {
      append(
        query.trim()
          // replace all special chars except letters, numbers and whitespace
          .replace("[^\\p{L}0-9\\s]".toRegex(), " ")
          // replace all whitespace sequences with *
          .split("\\s+".toRegex())
          .filter { it.isNotEmpty() }
          .joinToString("*")
      )
      append('*')
    }
    
    return dao.search(searchString).mapNotNull { repo.get(it) }
  }
}

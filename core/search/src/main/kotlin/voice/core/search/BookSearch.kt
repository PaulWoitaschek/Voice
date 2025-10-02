package voice.core.search

import dev.zacsweers.metro.Inject
import voice.core.data.Book
import voice.core.data.repo.BookRepository
import voice.core.data.repo.internals.dao.BookContentDao
import voice.core.logging.api.Logger

@Inject
class BookSearch(
  private val dao: BookContentDao,
  private val repo: BookRepository,
) {
  suspend fun search(query: String): List<Book> {
    val matchQuery = buildString {
      append(
        query.trim()
          // replace all special chars except umlauts and other language specific letters
          .replace("[^\\p{L}0-9\\s]".toRegex(), " ")
          // replace all whitespaces with *
          .split("\\s+".toRegex()).filter { it.isNotEmpty() }.joinToString("*"),
      )
      append('*')
    }
    Logger.d("search with MATCH: $matchQuery")
    return dao.search(matchQuery).mapNotNull { repo.get(it) }
  }
}

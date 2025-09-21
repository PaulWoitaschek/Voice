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

  suspend fun search2(query: String): List<Book> {
    val searchString = buildString {
      append(
        query.trim()
          // replace all special chars except umlauts and other language specific letters
          .replace("[^\\p{L}0-9\\s]".toRegex(), " ")
          // replace all whitespaces with *
          .split("\\s+".toRegex()).joinToString("*"),
      )
      append('*')
    }
    val results = dao.search(searchString)

    return results.mapNotNull { repo.get(it) }
  }

  suspend fun search(query: String): List<Book> {
    return dao.search(
      buildString {
        append(
          query.trim()
            // replace all special chars except umlauts and other language specific letters
            .replace("[^\\p{L}0-9\\s]".toRegex(), " ")
            // replace all whitespaces with *
            .split("\\s+".toRegex()).joinToString("*"),
        )
        append('*')
      },
    ).mapNotNull { repo.get(it) }
  }
}

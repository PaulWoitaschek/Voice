package voice.features.bookOverview.editBook

import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book

data class NameSuggestion(
  val name: String,
  val bookCount: Int,
)

// names that only differ in case are one, in the spelling most books use
internal fun List<Book>.nameSuggestions(name: (Book) -> String?): List<NameSuggestion> {
  return mapNotNull { book -> name(book)?.trim()?.takeIf { it.isNotEmpty() } }
    .groupBy { it.lowercase() }
    .map { (_, names) ->
      NameSuggestion(
        name = names.groupingBy { it }.eachCount().maxBy { it.value }.key,
        bookCount = names.size,
      )
    }
    .sortedWith(compareBy(NaturalOrderComparator.stringComparator) { it.name.lowercase() })
}

internal fun List<NameSuggestion>.matching(typed: String): List<NameSuggestion> {
  val query = typed.trim()
  if (query.isEmpty()) return emptyList()
  return filter {
      it.name.contains(query, ignoreCase = true) && !it.name.equals(query, ignoreCase = true)
    }
    .sortedByDescending { it.name.startsWith(query, ignoreCase = true) }
    .take(3)
}

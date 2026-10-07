package voice.core.search

import dev.zacsweers.metro.Inject
import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book
import voice.core.data.repo.BookRepository
import voice.core.data.repo.internals.dao.BookContentDao
import voice.core.logging.api.Logger
import java.util.concurrent.TimeUnit

@Inject
class BookSearch(
  private val dao: BookContentDao,
  private val repo: BookRepository,
) {

  /**
   * Searches the active books for all words of the [query], each matching the start of a word in any field.
   * The best matches come first.
   */
  suspend fun search(query: String): List<BookSearchResult> {
    val terms = SearchText(query).words.map { it.text }.distinct()
    if (terms.isEmpty()) return emptyList()

    // the full text search removes the diacritics itself, and decomposing would break scripts like Hangul
    val matchQuery = SearchText(query, removeDiacritics = false).words
      .joinToString(separator = " ") { "${it.text}*" }
    Logger.d("search with MATCH: $matchQuery")
    val candidates = dao.search(matchQuery).toSet()
    if (candidates.isEmpty()) return emptyList()

    return repo.all()
      .filter { it.id in candidates }
      .map { book -> rank(book, terms) }
      .sortedWith(resultComparator)
  }

  private fun rank(
    book: Book,
    terms: List<String>,
  ): BookSearchResult {
    val texts = BookSearchField.entries.mapNotNull { field ->
      field.text(book.content)?.let { field to it }
    }.toMap()
    val words = texts.mapValues { (_, text) -> SearchText(text).words }
    val matches = mutableMapOf<BookSearchField, MutableList<IntRange>>()
    val matchedTerms = mutableMapOf<BookSearchField, MutableSet<String>>()
    var score = 0
    terms.forEach { term ->
      var bestTermScore = 0
      words.forEach { (field, fieldWords) ->
        fieldWords.forEach { word ->
          if (word.text.startsWith(term)) {
            matches.getOrPut(field) { mutableListOf() } += word.originalRange(term.length)
            matchedTerms.getOrPut(field) { mutableSetOf() } += term
            val quality = if (word.text.length == term.length) WHOLE_WORD else WORD_START
            bestTermScore = maxOf(bestTermScore, quality + field.weight)
          }
        }
      }
      score += bestTermScore
    }
    return BookSearchResult(
      book = book,
      matches = matches.mapValues { (field, ranges) -> ranges.merged(texts.getValue(field)) },
      fieldsMatchingAllWords = matchedTerms.filterValues { it.size == terms.size }.keys,
      score = score,
    )
  }

  private companion object {
    const val WHOLE_WORD = 20
    const val WORD_START = 10

    val resultComparator: Comparator<BookSearchResult> = compareByDescending<BookSearchResult> { it.score }
      .thenByDescending { it.book.isInProgress() }
      .thenByDescending { it.book.content.lastPlayedAt }
      .thenBy(NaturalOrderComparator.stringComparator) { it.book.content.part.orEmpty() }
      .thenBy(NaturalOrderComparator.stringComparator) { it.book.content.name }
  }
}

private val BookSearchField.weight: Int
  get() = when (this) {
    BookSearchField.Title -> 5
    BookSearchField.Author -> 4
    BookSearchField.Series -> 3
    BookSearchField.Narrator -> 2
    BookSearchField.Genre -> 1
  }

private fun Book.isInProgress(): Boolean {
  return position > 0 && position < duration - TimeUnit.SECONDS.toMillis(5)
}

/**
 * Joins overlapping ranges, and ranges only apart by spaces, so "stephen king" is highlighted as one.
 */
private fun List<IntRange>.merged(text: String): List<IntRange> {
  val result = mutableListOf<IntRange>()
  sortedBy { it.first }.forEach { range ->
    val last = result.lastOrNull()
    if (last != null && text.substring(minOf(last.last + 1, range.first), range.first).isBlank()) {
      result[result.lastIndex] = last.first..maxOf(last.last, range.last)
    } else {
      result += range
    }
  }
  return result
}

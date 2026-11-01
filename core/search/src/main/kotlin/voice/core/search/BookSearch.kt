package voice.core.search

import dev.zacsweers.metro.Inject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import voice.core.common.DispatcherProvider
import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book
import voice.core.data.repo.BookRepository

@Inject
class BookSearch(
  private val repo: BookRepository,
  private val dispatcherProvider: DispatcherProvider,
) {

  // splitting texts into words takes longer than matching them, and the search runs again on every
  // keystroke and
  // every position update of the playing book
  private var wordsByText = emptyMap<String, List<SearchText.Word>>()

  suspend fun search(query: String): List<BookSearchResult> {
    val terms = SearchText(query).words.map { it.text }.distinct()
    if (terms.isEmpty()) return emptyList()
    val allBooks = repo.all()
    return withContext(dispatcherProvider.default) {
      val previousWords = wordsByText
      val words = HashMap<String, List<SearchText.Word>>()
      val books = allBooks.map { book ->
        SearchableBook(book) { text ->
          words.getOrPut(text) { previousWords[text] ?: SearchText(text).words }
        }
      }
      wordsByText = words
      // typos only count when nothing matches as typed, so similar words don't pile up below the
      // right books
      books
        .mapNotNull { rank(it, terms, typos = false) }
        .ifEmpty { books.mapNotNull { rank(it, terms, typos = true) } }
        .sortedWith(resultComparator)
    }
  }

  private fun rank(
    book: SearchableBook,
    terms: List<String>,
    typos: Boolean,
  ): BookSearchResult? {
    val matches = mutableMapOf<BookSearchField, MutableList<IntRange>>()
    val matchedTerms = mutableMapOf<BookSearchField, MutableSet<String>>()
    var score = 0
    for (term in terms) {
      var bestTermScore = 0
      book.words.forEach { (field, fieldWords) ->
        fieldWords.forEach { word ->
          val match = match(word.text, term, typos)
          if (match != null) {
            matches.getOrPut(field) { mutableListOf() } += word.originalRange(match.length)
            matchedTerms.getOrPut(field) { mutableSetOf() } += term
            bestTermScore = maxOf(bestTermScore, match.quality + field.weight)
          }
        }
      }
      if (bestTermScore == 0 && book.fileWords.none { match(it.text, term, typos) != null })
        return null
      score += bestTermScore
    }
    return BookSearchResult(
      book = book.book,
      matches = matches.mapValues { (field, ranges) -> ranges.merged(book.texts.getValue(field)) },
      fieldsMatchingAllWords = matchedTerms.filterValues { it.size == terms.size }.keys,
      score = score,
    )
  }

  private fun match(
    word: String,
    term: String,
    typos: Boolean,
  ): WordMatch? =
    when {
      word.startsWith(term) ->
        WordMatch(term.length, if (word.length == term.length) WHOLE_WORD else WORD_START)
      typos && term.length >= MIN_TYPO_LENGTH ->
        word.typoPrefixLength(term)?.let { WordMatch(it, TYPO) }
      else -> null
    }

  private class SearchableBook(
    val book: Book,
    split: (String) -> List<SearchText.Word>,
  ) {
    val texts =
      BookSearchField.entries
        .mapNotNull { field ->
          field.text(book)?.let { field to it }
        }
        .toMap()
    val words = texts.mapValues { (_, text) -> split(text) }

    // a book the listener edited is still found by what its files say, which isn't shown, so it
    // ranks below
    // visible matches
    val fileWords =
      listOfNotNull(
          book.content.metadata.name.takeIf { book.custom.name != null },
          book.content.metadata.author?.takeIf { book.custom.author != null },
        )
        .flatMap(split)
  }

  private class WordMatch(
    val length: Int,
    val quality: Int,
  )

  private companion object {
    const val WHOLE_WORD = 20
    const val WORD_START = 10
    const val TYPO = 5

    // shorter words are one typo away from too many others
    const val MIN_TYPO_LENGTH = 5

    val resultComparator: Comparator<BookSearchResult> =
      compareByDescending<BookSearchResult> { it.score }
        .thenByDescending { it.book.isInProgress() }
        .thenByDescending { it.book.content.lastPlayedAt }
        .thenBy(NaturalOrderComparator.stringComparator) { it.book.part.orEmpty() }
        .thenBy(NaturalOrderComparator.stringComparator) { it.book.name }
  }
}

private val BookSearchField.weight: Int
  get() =
    when (this) {
      BookSearchField.Title -> 5
      BookSearchField.Author -> 4
      BookSearchField.Series -> 3
      BookSearchField.Narrator -> 2
      BookSearchField.Genre -> 1
    }

private fun Book.isInProgress(): Boolean {
  return position > 0 && position < duration - TimeUnit.SECONDS.toMillis(5)
}

// ranges only apart by spaces are joined too, so "stephen king" is highlighted as one
private fun List<IntRange>.merged(text: String): List<IntRange> {
  val result = mutableListOf<IntRange>()
  sortedBy { it.first }
    .forEach { range ->
      val last = result.lastOrNull()
      if (
        last != null && text.substring(minOf(last.last + 1, range.first), range.first).isBlank()
      ) {
        result[result.lastIndex] = last.first..maxOf(last.last, range.last)
      } else {
        result += range
      }
    }
  return result
}

package voice.core.search

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext
import voice.core.common.DispatcherProvider
import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.data.category
import voice.core.data.displayName
import voice.core.data.repo.BookRepository

@Inject
class BookSearch(
  private val repo: BookRepository,
  private val dispatcherProvider: DispatcherProvider,
) {

  // splitting texts into words takes longer than matching them, and the search runs again on every
  // keystroke and every position update of the playing book
  private var searchableBooks = emptyMap<BookId, SearchableBook>()

  suspend fun search(query: String): List<BookSearchResult> {
    val terms = SearchText(query).words.map { it.text }.distinct()
    if (terms.isEmpty()) return emptyList()
    val allBooks = repo.all()
    return withContext(dispatcherProvider.default) {
      val previous = searchableBooks
      val books = allBooks.map { book ->
        previous[book.id]?.takeIf { it.book == book } ?: SearchableBook(book)
      }
      searchableBooks = books.associateBy { it.book.id }
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
    val matches = mutableMapOf<Int, MutableList<IntRange>>()
    val matchedTerms = mutableMapOf<BookSearchField, MutableSet<String>>()
    var score = 0
    for (term in terms) {
      var bestTermScore = 0
      book.texts.forEachIndexed { index, (field, _) ->
        book.words[index].forEach { word ->
          val match = match(word.text, term, typos)
          if (match != null) {
            matches.getOrPut(index) { mutableListOf() } += word.originalRange(match.length)
            matchedTerms.getOrPut(field) { mutableSetOf() } += term
            bestTermScore = maxOf(bestTermScore, field.score(match.quality))
          }
        }
      }
      if (bestTermScore == 0 && book.fileWords.none { match(it.text, term, typos) != null })
        return null
      score += bestTermScore
    }
    // of the chapters, the first one that matched is shown
    val shown = matches.keys.sorted().distinctBy { book.texts[it].first }
    return BookSearchResult(
      book = book.book,
      matches =
        shown.associate { index ->
          val (field, text) = book.texts[index]
          field to matches.getValue(index).merged(text)
        },
      fieldsMatchingAllWords = matchedTerms.filterValues { it.size == terms.size }.keys,
      score = score,
      chapter =
        shown.firstNotNullOfOrNull { index ->
          book.texts[index].takeIf { it.first == BookSearchField.Chapters }?.second
        },
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

  private class SearchableBook(val book: Book) {
    val texts =
      BookSearchField.entries.mapNotNull { field -> field.text(book)?.let { field to it } } +
        book.chapterNames().map { BookSearchField.Chapters to it }
    val words = texts.map { (_, text) -> SearchText(text).words }

    // a book the listener edited is still found by what its files say, which isn't shown, so it
    // ranks below
    // visible matches
    val fileWords =
      listOfNotNull(
          book.content.metadata.name.takeIf { book.custom.name != null },
          book.content.metadata.author?.takeIf { book.custom.author != null },
        )
        .flatMap { SearchText(it).words }
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
        .thenByDescending { it.book.category == BookCategory.CURRENT }
        .thenByDescending { it.book.content.lastPlayedAt }
        .thenBy(NaturalOrderComparator.stringComparator) { it.book.part.orEmpty() }
        .thenBy(NaturalOrderComparator.stringComparator) { it.book.name }
  }
}

private fun BookSearchField.score(quality: Int): Int =
  when (this) {
    BookSearchField.Title -> quality + 5
    BookSearchField.Author -> quality + 4
    BookSearchField.Series -> quality + 3
    BookSearchField.Narrator -> quality + 2
    BookSearchField.Genre -> quality + 1
    // below a typo in any other field, but still above zero, which is no match
    BookSearchField.Chapters -> quality / 5
  }

// the names the chapter list shows, so a chapter it calls "Chapter 3" isn't found by that
private fun Book.chapterNames(): List<String> {
  val marks = chapters.flatMap { it.chapterMarks }
  return marks.mapIndexedNotNull { index, mark -> mark.displayName(index + 1, marks.size) }
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

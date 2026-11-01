package voice.features.bookOverview.overview

import java.text.Normalizer
import java.time.Instant
import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book
import voice.core.data.BookComparator

internal fun seriesKey(name: String): String? {
  return Normalizer.normalize(name, Normalizer.Form.NFKD)
    // accents like the one of ä can go, but in scripts like Devanagari the marks are part of the letters
    .filter { it.isLetterOrDigit() || (it.category in Marks && it !in Accents) }
    .lowercase()
    .takeIf { it.isNotEmpty() }
}

private val Marks = setOf(CharCategory.NON_SPACING_MARK, CharCategory.COMBINING_SPACING_MARK, CharCategory.ENCLOSING_MARK)
private val Accents = '\u0300'..'\u036F'

internal val Book.seriesName: String?
  get() = series?.trim()?.takeIf { it.isNotEmpty() }

internal val Book.seriesPart: String?
  get() = part?.trim()?.takeIf { it.isNotEmpty() }

internal val Book.seriesLine: String?
  get() = seriesName?.let { name -> listOfNotNull(name, seriesPart).joinToString(" ") }

// Voice doesn't store file names, so books without a part follow their names, which mostly come from their files
internal val SeriesOrder: Comparator<Book> =
  compareBy<Book, String?>(nullsLast(NaturalOrderComparator.stringComparator)) { it.seriesPart }
    .then(BookComparator.ByName)
    .thenBy { it.id.value }

internal class Series(
  val key: String,
  val name: String,
  val books: List<Book>,
) {

  val author: String? = books.mapNotNull { it.author }.mostCommon()

  val finishedCount: Int = books.count { it.category == BookOverviewCategory.FINISHED }

  val category: BookOverviewCategory =
    when {
      finishedCount == books.size -> BookOverviewCategory.FINISHED
      books.all { it.category == BookOverviewCategory.NOT_STARTED } -> BookOverviewCategory.NOT_STARTED
      else -> BookOverviewCategory.CURRENT
    }

  // the series screen lists hidden books too, but doesn't offer to continue one
  val inProgress: Book? =
    books.filter { it.category == BookOverviewCategory.CURRENT && !it.custom.hidden }.maxByOrNull { it.content.lastPlayedAt }

  val firstUnfinished: Book? = books.firstOrNull { it.category != BookOverviewCategory.FINISHED && !it.custom.hidden }

  val front: Book = inProgress ?: firstUnfinished ?: books.first()

  val covers: List<String?> = (listOf(front) + books.filter { it != front }.take(2)).map { it.coverUrl }

  val lastPlayedAt: Instant = books.maxOf { it.content.lastPlayedAt }

  val progress: List<Float> = books.map { if (it.category == BookOverviewCategory.FINISHED) 1F else it.progress() }
}

internal fun List<Book>.series(): List<Series> {
  return mapNotNull { book -> book.seriesName?.let(::seriesKey)?.let { key -> key to book } }
    .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    .map { (key, books) ->
      Series(
        key = key,
        name = books.mapNotNull { it.seriesName }.mostCommon()!!,
        books = books.sortedWith(SeriesOrder),
      )
    }
}

internal sealed interface LibraryEntry {

  data class Single(val book: Book) : LibraryEntry

  data class Stack(val series: Series) : LibraryEntry
}

// a started series stays one stack too, so none of its books disappears from the library
internal fun libraryEntries(books: List<Book>): Map<BookOverviewCategory, List<LibraryEntry>> {
  val stacks = books.series().filter { it.books.size >= 2 }
  val stacked = stacks.flatMapTo(HashSet()) { series -> series.books.map { it.id } }
  val entries = books.filter { it.id !in stacked }.map { LibraryEntry.Single(it) } + stacks.map { LibraryEntry.Stack(it) }
  return entries.groupBy { it.category }.mapValues { (category, entries) -> entries.sortedWith(category.entryComparator()) }.toSortedMap()
}

private val LibraryEntry.category: BookOverviewCategory
  get() =
    when (this) {
      is LibraryEntry.Single -> book.category
      is LibraryEntry.Stack -> series.category
    }

private val LibraryEntry.lastPlayedAt: Instant
  get() =
    when (this) {
      is LibraryEntry.Single -> book.content.lastPlayedAt
      is LibraryEntry.Stack -> series.lastPlayedAt
    }

private val LibraryEntry.name: String
  get() =
    when (this) {
      is LibraryEntry.Single -> book.name
      is LibraryEntry.Stack -> series.name
    }

private fun BookOverviewCategory.entryComparator(): Comparator<LibraryEntry> =
  when (this) {
    BookOverviewCategory.CURRENT,
    BookOverviewCategory.FINISHED -> compareByDescending { it.lastPlayedAt }
    BookOverviewCategory.NOT_STARTED -> compareBy(NaturalOrderComparator.stringComparator) { it.name }
  }

private fun List<String>.mostCommon(): String? {
  return groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
}

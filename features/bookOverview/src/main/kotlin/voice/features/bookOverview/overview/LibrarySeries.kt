package voice.features.bookOverview.overview

import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book
import voice.core.data.BookComparator
import java.text.Normalizer
import java.time.Instant

// "Harry Potter", "harry-potter" and "Härry Potter" are one series
internal fun seriesKey(name: String): String? {
  return Normalizer.normalize(name, Normalizer.Form.NFKD)
    .filter { it.isLetterOrDigit() }
    .lowercase()
    .takeIf { it.isNotEmpty() }
}

internal val Book.seriesName: String?
  get() = series?.trim()?.takeIf { it.isNotEmpty() }

internal val Book.seriesPart: String?
  get() = part?.trim()?.takeIf { it.isNotEmpty() }

internal val Book.seriesLine: String?
  get() = seriesName?.let { name -> listOfNotNull(name, seriesPart).joinToString(" ") }

// Voice doesn't store file names, so books without a part follow their names, which mostly come from their files
internal val SeriesOrder: Comparator<Book> = compareBy<Book, String?>(nullsLast(NaturalOrderComparator.stringComparator)) { it.seriesPart }
  .then(BookComparator.ByName)
  .thenBy { it.id.value }

internal class Series(
  val key: String,
  val name: String,
  val books: List<Book>,
) {

  val author: String? = books.mapNotNull { it.author }.mostCommon()

  val finishedCount: Int = books.count { it.category == BookOverviewCategory.FINISHED }

  val category: BookOverviewCategory = when {
    finishedCount == books.size -> BookOverviewCategory.FINISHED
    books.all { it.category == BookOverviewCategory.NOT_STARTED } -> BookOverviewCategory.NOT_STARTED
    else -> BookOverviewCategory.CURRENT
  }

  val inProgress: Book? = books
    .filter { it.category == BookOverviewCategory.CURRENT }
    .maxByOrNull { it.content.lastPlayedAt }

  val firstUnfinished: Book? = books.firstOrNull { it.category != BookOverviewCategory.FINISHED }

  val front: Book = inProgress ?: firstUnfinished ?: books.first()

  val covers: List<String?> = (listOf(front) + books.filter { it != front }.take(2)).map { it.coverUrl }

  val lastPlayedAt: Instant = books.maxOf { it.content.lastPlayedAt }

  val progress: Float = run {
    val duration = books.sumOf { it.duration }
    val listened = books.sumOf { if (it.category == BookOverviewCategory.FINISHED) it.duration else it.position }
    if (duration > 0) (listened.toFloat() / duration).coerceIn(0F, 1F) else 0F
  }
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

/** Two or more books of a series are one stack, wherever their progress is. */
internal fun libraryEntries(books: List<Book>): Map<BookOverviewCategory, List<LibraryEntry>> {
  val stacks = books.series().filter { it.books.size >= 2 }
  val stacked = stacks.flatMapTo(HashSet()) { series -> series.books.map { it.id } }
  val entries = books.filter { it.id !in stacked }.map { LibraryEntry.Single(it) } +
    stacks.map { LibraryEntry.Stack(it) }
  return entries
    .groupBy { it.category }
    .mapValues { (category, entries) -> entries.sortedWith(category.entryComparator()) }
    .toSortedMap()
}

private val LibraryEntry.category: BookOverviewCategory
  get() = when (this) {
    is LibraryEntry.Single -> book.category
    is LibraryEntry.Stack -> series.category
  }

private val LibraryEntry.lastPlayedAt: Instant
  get() = when (this) {
    is LibraryEntry.Single -> book.content.lastPlayedAt
    is LibraryEntry.Stack -> series.lastPlayedAt
  }

private val LibraryEntry.name: String
  get() = when (this) {
    is LibraryEntry.Single -> book.name
    is LibraryEntry.Stack -> series.name
  }

private fun BookOverviewCategory.entryComparator(): Comparator<LibraryEntry> = when (this) {
  BookOverviewCategory.CURRENT, BookOverviewCategory.FINISHED -> compareByDescending { it.lastPlayedAt }
  BookOverviewCategory.NOT_STARTED -> compareBy(NaturalOrderComparator.stringComparator) { it.name }
}

// a tie goes to the spelling that comes first
private fun List<String>.mostCommon(): String? {
  return groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
}

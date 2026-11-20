package voice.features.bookOverview.overview

import kotlin.time.Instant
import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.SeriesOrder
import voice.core.data.category
import voice.core.data.nameKey
import voice.core.data.seriesName

internal class Series(
  val key: String,
  val name: String,
  val books: List<Book>,
) {

  val author: String? = books.mapNotNull { it.author }.mostCommon()

  val finishedCount: Int = books.count { it.category == BookCategory.FINISHED }

  val category: BookCategory =
    when {
      finishedCount == books.size -> BookCategory.FINISHED
      books.all { it.category == BookCategory.NOT_STARTED } -> BookCategory.NOT_STARTED
      else -> BookCategory.CURRENT
    }

  // the series screen lists hidden books too, but doesn't offer to continue one
  val inProgress: Book? =
    books
      .filter { it.category == BookCategory.CURRENT && !it.custom.hidden }
      .maxByOrNull { it.content.lastPlayedAt }

  val firstUnfinished: Book? = books.firstOrNull {
    it.category != BookCategory.FINISHED && !it.custom.hidden
  }

  val front: Book = inProgress ?: firstUnfinished ?: books.first()

  val covers: List<String?> =
    (listOf(front) + books.filter { it != front }.take(2)).map { it.coverUrl }

  val lastPlayedAt: Instant = books.maxOf { it.content.lastPlayedAt }

  val progress: List<Float> = books.map {
    if (it.category == BookCategory.FINISHED) 1F else it.progress()
  }

  val hidden: Boolean = books.all { it.custom.hidden }
}

internal fun List<Book>.inSeries(name: String): List<Book> {
  val key = nameKey(name) ?: return emptyList()
  return filter { book -> book.seriesName?.let(::nameKey) == key }
}

internal fun List<Book>.series(): List<Series> {
  return mapNotNull { book -> book.seriesName?.let(::nameKey)?.let { key -> key to book } }
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
internal fun libraryEntries(books: List<Book>): Map<BookCategory, List<LibraryEntry>> {
  val stacks = books.series().filter { it.books.size >= 2 }
  val stacked = stacks.flatMapTo(HashSet()) { series -> series.books.map { it.id } }
  val entries =
    books.filter { it.id !in stacked }.map { LibraryEntry.Single(it) } +
      stacks.map { LibraryEntry.Stack(it) }
  return entries
    .groupBy { it.category }
    .mapValues { (category, entries) -> entries.sortedWith(category.entryComparator()) }
    .toSortedMap()
}

private val LibraryEntry.category: BookCategory
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

private fun BookCategory.entryComparator(): Comparator<LibraryEntry> =
  when (this) {
    BookCategory.CURRENT,
    BookCategory.FINISHED -> compareByDescending { it.lastPlayedAt }
    BookCategory.NOT_STARTED -> compareBy(NaturalOrderComparator.stringComparator) { it.name }
  }

internal fun <T> List<T>.mostCommon(): T? {
  return groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
}

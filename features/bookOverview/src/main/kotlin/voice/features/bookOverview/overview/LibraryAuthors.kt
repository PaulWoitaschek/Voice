package voice.features.bookOverview.overview

import java.time.Instant
import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book
import voice.core.data.BookCategory

// files often name one of these in place of an empty author tag
private val PlaceholderAuthors = setOf("unknown", "unknownauthor", "unknownartist")

internal fun authorKey(author: String?): String? {
  return author?.let(::nameKey)?.takeIf { it !in PlaceholderAuthors }
}

internal val Book.authorKey: String?
  get() = authorKey(author)

// the whole series too, like in the library
internal fun List<Book>.byAuthor(name: String?): List<Book> {
  val key = authorKey(name)
  val series =
    series()
      .filter { series -> series.books.mapNotNull { it.authorKey }.mostCommon() == key }
      .flatMapTo(HashSet()) { series -> series.books.map { it.id } }
  return filter { it.authorKey == key || it.id in series }
}

internal class AuthorGroup(
  val key: String?,
  val name: String?,
  val entries: List<LibraryEntry>,
) {

  val books: List<Book> = entries.flatMap { it.books }

  val seriesCount: Int = entries.count { it is LibraryEntry.Stack }

  // one of each series first, so a long series doesn't take all the places
  val covers: List<String?> =
    (entries.map { it.front } + books).distinctBy { it.id }.take(4).map { it.coverUrl }

  val lastPlayedAt: Instant = books.maxOf { it.content.lastPlayedAt }

  val hidden: Boolean = books.all { it.custom.hidden }
}

internal fun Map<BookCategory, List<LibraryEntry>>.groupedByAuthor():
  Map<BookCategory, List<AuthorGroup>> {
  val books = values.flatten().flatMap { it.books }
  val keys = books.associate { it.id to it.authorKey }
  // the same spelling in every section
  val names =
    books.groupBy({ keys[it.id] }, { it.author?.trim() }).mapValues { (key, names) ->
      if (key == null) null else names.filterNotNull().mostCommon()
    }
  // a tap in Current continues a book, and an author's entry would hide which one
  return filterKeys { it != BookCategory.CURRENT }
    .mapValues { (category, entries) ->
      entries
        // a series goes to the author most of its books have
        .groupBy { entry -> entry.books.mapNotNull { keys[it.id] }.mostCommon() }
        .map { (key, entries) -> AuthorGroup(key, names[key], entries) }
        .sortedWith(category.authorComparator())
    }
}

private val LibraryEntry.books: List<Book>
  get() =
    when (this) {
      is LibraryEntry.Single -> listOf(book)
      is LibraryEntry.Stack -> series.books
    }

private val LibraryEntry.front: Book
  get() =
    when (this) {
      is LibraryEntry.Single -> book
      is LibraryEntry.Stack -> series.front
    }

private fun BookCategory.authorComparator(): Comparator<AuthorGroup> {
  val order: Comparator<AuthorGroup> =
    when (this) {
      BookCategory.NOT_STARTED ->
        compareBy(NaturalOrderComparator.stringComparator) { it.name.orEmpty() }
      BookCategory.CURRENT,
      BookCategory.FINISHED -> compareByDescending { it.lastPlayedAt }
    }
  return compareBy<AuthorGroup> { it.key == null }.then(order)
}

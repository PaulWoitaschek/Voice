package voice.features.bookOverview.overview

import voice.core.data.Book
import voice.features.bookOverview.book
import voice.features.bookOverview.chapter
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class LibrarySeriesTest {

  @Test
  fun `the only book of a series stays a book`() {
    val book = inProgress(series = "Saga")

    assertEquals(
      expected = mapOf(BookOverviewCategory.CURRENT to listOf(LibraryEntry.Single(book))),
      actual = libraryEntries(listOf(book)),
    )
  }

  @Test
  fun `the books of a series are one stack, wherever their progress is`() {
    val first = finished(series = "Saga", part = "1")
    val second = inProgress(series = "Saga", part = "2")
    val third = notStarted(series = "Saga", part = "3")
    val other = notStarted(name = "Other")

    val entries = libraryEntries(listOf(third, other, second, first))

    assertEquals(expected = listOf(BookOverviewCategory.CURRENT, BookOverviewCategory.NOT_STARTED), actual = entries.keys.toList())
    assertEquals(expected = listOf(first, second, third), actual = entries.getValue(BookOverviewCategory.CURRENT).stack().books)
    assertEquals(expected = listOf(LibraryEntry.Single(other)), actual = entries.getValue(BookOverviewCategory.NOT_STARTED))
  }

  @Test
  fun `a series that is not started is not started`() {
    val entries = libraryEntries(listOf(notStarted(series = "Saga"), notStarted(series = "Saga")))

    assertEquals(expected = setOf(BookOverviewCategory.NOT_STARTED), actual = entries.keys)
  }

  @Test
  fun `a series with every book finished is completed`() {
    val entries = libraryEntries(listOf(finished(series = "Saga"), finished(series = "Saga")))

    assertEquals(expected = setOf(BookOverviewCategory.FINISHED), actual = entries.keys)
  }

  @Test
  fun `a series between two books is current`() {
    val entries = libraryEntries(listOf(finished(series = "Saga"), notStarted(series = "Saga")))

    assertEquals(expected = setOf(BookOverviewCategory.CURRENT), actual = entries.keys)
  }

  @Test
  fun `series names match regardless of case, accents and punctuation`() {
    val series = listOf(
      notStarted(series = "Harry Potter"),
      notStarted(series = " harry-potter "),
      notStarted(series = "Härry Potter"),
      notStarted(series = "Harry Potter"),
    ).series()

    assertEquals(expected = 1, actual = series.size)
    assertEquals(expected = "Harry Potter", actual = series.single().name)
  }

  @Test
  fun `a series without letters or digits is no series`() {
    assertEquals(expected = emptyList(), actual = listOf(notStarted(series = "–"), notStarted(series = "–")).series())
  }

  @Test
  fun `books follow their part, then the books without one follow their names`() {
    val part10 = notStarted(series = "Saga", part = "10")
    val part2 = notStarted(series = "Saga", part = "2")
    val part1 = notStarted(series = "Saga", part = "1")
    val chapter10 = notStarted(series = "Saga", name = "Chapter 10")
    val chapter9 = notStarted(series = "Saga", name = "Chapter 9")

    val series = listOf(chapter10, part10, chapter9, part2, part1).series().single()

    assertEquals(expected = listOf(part1, part2, part10, chapter9, chapter10), actual = series.books)
  }

  @Test
  fun `the front is the book in progress played last`() {
    val first = inProgress(series = "Saga", part = "1", lastPlayedAt = Instant.ofEpochSecond(1))
    val second = inProgress(series = "Saga", part = "2", lastPlayedAt = Instant.ofEpochSecond(3))
    val third = finished(series = "Saga", part = "3", lastPlayedAt = Instant.ofEpochSecond(5))

    val series = listOf(first, second, third).series().single()

    assertEquals(expected = second, actual = series.front)
    assertEquals(expected = second, actual = series.inProgress)
    assertEquals(expected = Instant.ofEpochSecond(5), actual = series.lastPlayedAt)
  }

  @Test
  fun `without a book in progress the front is the first book not finished`() {
    val first = finished(series = "Saga", part = "1")
    val second = notStarted(series = "Saga", part = "2")
    val third = notStarted(series = "Saga", part = "3")

    val series = listOf(third, second, first).series().single()

    assertEquals(expected = null, actual = series.inProgress)
    assertEquals(expected = second, actual = series.front)
    assertEquals(expected = second, actual = series.firstUnfinished)
  }

  @Test
  fun `a finished series shows its first book`() {
    val first = finished(series = "Saga", part = "1")
    val second = finished(series = "Saga", part = "2")

    val series = listOf(second, first).series().single()

    assertEquals(expected = null, actual = series.firstUnfinished)
    assertEquals(expected = first, actual = series.front)
  }

  @Test
  fun `progress weighs the books by their length`() {
    val short = book(series = "Saga", part = "1", chapters = listOf(chapter(duration = 10_000)), time = 10_000)
    val long = book(series = "Saga", part = "2", chapters = listOf(chapter(duration = 10_000), chapter(duration = 10_000)), time = 5_000)

    val series = listOf(short, long).series().single()

    assertEquals(expected = 0.5F, actual = series.progress)
    assertEquals(expected = 1, actual = series.finishedCount)
  }

  @Test
  fun `the author is the one most books name`() {
    val series = listOf(
      notStarted(series = "Saga", author = "Daniel Hartwell"),
      notStarted(series = "Saga", author = "D. Hartwell"),
      notStarted(series = "Saga", author = "Daniel Hartwell"),
      notStarted(series = "Saga", author = null),
    ).series().single()

    assertEquals(expected = "Daniel Hartwell", actual = series.author)
  }

  @Test
  fun `current stacks and books are sorted by when they were played last`() {
    val book = inProgress(name = "Book", lastPlayedAt = Instant.ofEpochSecond(2))
    val sagaFirst = finished(series = "Saga", lastPlayedAt = Instant.ofEpochSecond(1))
    val sagaSecond = inProgress(series = "Saga", lastPlayedAt = Instant.ofEpochSecond(3))
    val newest = inProgress(name = "Newest", lastPlayedAt = Instant.ofEpochSecond(4))

    val current = libraryEntries(listOf(book, sagaFirst, sagaSecond, newest)).getValue(BookOverviewCategory.CURRENT)

    assertEquals(expected = listOf("Newest", "Saga", "Book"), actual = current.map { it.title() })
  }

  @Test
  fun `stacks and books not started yet are sorted by name`() {
    val entries = libraryEntries(
      listOf(
        notStarted(name = "Part 10"),
        notStarted(series = "Part 3"),
        notStarted(name = "Part 2"),
        notStarted(series = "Part 3"),
      ),
    )

    assertEquals(
      expected = listOf("Part 2", "Part 3", "Part 10"),
      actual = entries.getValue(BookOverviewCategory.NOT_STARTED).map { it.title() },
    )
  }

  private fun List<LibraryEntry>.stack(): Series = (single() as LibraryEntry.Stack).series

  private fun LibraryEntry.title(): String = when (this) {
    is LibraryEntry.Single -> book.name
    is LibraryEntry.Stack -> series.name
  }

  private fun notStarted(
    series: String? = null,
    part: String? = null,
    name: String = "Book",
    author: String? = "Author",
  ): Book = book(series = series, part = part, name = name, author = author, time = 0)

  private fun inProgress(
    series: String? = null,
    part: String? = null,
    name: String = "Book",
    lastPlayedAt: Instant = Instant.EPOCH,
  ): Book = book(series = series, part = part, name = name, author = "Author", time = 5_000, lastPlayedAt = lastPlayedAt)

  // the default book has two chapters of 10 seconds
  private fun finished(
    series: String? = null,
    part: String? = null,
    lastPlayedAt: Instant = Instant.EPOCH,
  ): Book = book(series = series, part = part, name = "Book", author = "Author", time = 20_000, lastPlayedAt = lastPlayedAt)
}

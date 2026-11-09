package voice.features.bookOverview.overview

import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.features.bookOverview.book

class LibraryAuthorsTest {

  @Test
  fun `current is never grouped`() {
    val authors = groups(inProgress(author = "Lena Morgan"), notStarted(author = "Lena Morgan"))

    assertEquals(expected = setOf(BookCategory.NOT_STARTED), actual = authors.keys)
  }

  @Test
  fun `an author is one entry in each section with their books`() {
    val authors =
      groups(
        notStarted(author = "Lena Morgan"),
        notStarted(author = "Lena Morgan"),
        finished(author = "Lena Morgan"),
        notStarted(author = "Nora Whitfield"),
      )

    assertEquals(
      expected = listOf("Lena Morgan" to 2, "Nora Whitfield" to 1),
      actual = authors.getValue(BookCategory.NOT_STARTED).map { it.name to it.books.size },
    )
    assertEquals(
      expected = listOf("Lena Morgan" to 1),
      actual = authors.getValue(BookCategory.FINISHED).map { it.name to it.books.size },
    )
  }

  @Test
  fun `names that only differ in case, spaces or punctuation are one author`() {
    val authors =
      groups(
        notStarted(author = "J. K. Rowling"),
        notStarted(author = "j.k. rowling"),
        notStarted(author = "J. K. Rowling"),
      )

    val author = authors.getValue(BookCategory.NOT_STARTED).single()
    assertEquals(expected = "J. K. Rowling", actual = author.name)
    assertEquals(expected = 3, actual = author.books.size)
  }

  @Test
  fun `an author has the same spelling in every section`() {
    val authors =
      groups(
        notStarted(author = "J.K. Rowling"),
        finished(author = "J. K. Rowling"),
        finished(author = "J. K. Rowling"),
      )

    assertEquals(
      expected = listOf("J. K. Rowling", "J. K. Rowling"),
      actual = authors.values.map { it.single().name },
    )
  }

  @Test
  fun `a series stays one unit with the author most of its books have`() {
    val authors =
      groups(
        notStarted(series = "Saga", author = "Lena Morgan"),
        notStarted(series = "Saga", author = "Lena Morgan"),
        notStarted(series = "Saga", author = "Nora Whitfield"),
        notStarted(name = "Single", author = "Lena Morgan"),
      )

    val author = authors.getValue(BookCategory.NOT_STARTED).single()
    assertEquals(expected = "Lena Morgan", actual = author.name)
    assertEquals(expected = 1, actual = author.seriesCount)
    assertEquals(expected = 4, actual = author.books.size)
  }

  @Test
  fun `books without an author or with a placeholder are one entry at the end`() {
    val authors =
      groups(
        notStarted(author = null),
        notStarted(author = " "),
        notStarted(author = "<Unknown>"),
        notStarted(author = "Unknown Artist"),
        notStarted(author = "Zoe Young"),
      )

    val entries = authors.getValue(BookCategory.NOT_STARTED)
    assertEquals(expected = listOf("Zoe Young", null), actual = entries.map { it.name })
    assertNull(entries.last().key)
    assertEquals(expected = 4, actual = entries.last().books.size)
  }

  @Test
  fun `authors not started yet are sorted by name`() {
    val authors =
      groups(
        notStarted(author = "Author 10"),
        notStarted(author = "Author 2"),
        notStarted(author = "Author 3"),
      )

    assertEquals(
      expected = listOf("Author 2", "Author 3", "Author 10"),
      actual = authors.getValue(BookCategory.NOT_STARTED).map { it.name },
    )
  }

  @Test
  fun `completed authors are sorted by the book listened to last`() {
    val authors =
      groups(
        finished(author = "Old", lastPlayedAt = Instant.ofEpochSecond(1)),
        finished(author = "New", lastPlayedAt = Instant.ofEpochSecond(3)),
        finished(author = "Old", lastPlayedAt = Instant.ofEpochSecond(2)),
        finished(author = null, lastPlayedAt = Instant.ofEpochSecond(4)),
      )

    assertEquals(
      expected = listOf("New", "Old", null),
      actual = authors.getValue(BookCategory.FINISHED).map { it.name },
    )
  }

  @Test
  fun `an author shows one cover of each series first`() {
    val saga = List(3) { notStarted(series = "Saga", author = "Lena Morgan", cover = "saga$it") }
    val single = notStarted(name = "Single", author = "Lena Morgan", cover = "single")
    val author = groups(*saga.toTypedArray(), single).getValue(BookCategory.NOT_STARTED).single()

    assertEquals(expected = single.coverUrl, actual = author.covers[1])
    assertEquals(expected = 4, actual = author.covers.distinct().size)
  }

  @Test
  fun `the books of an author include those with a different spelling`() {
    val books =
      listOf(
        notStarted(author = "Lena Morgan"),
        notStarted(author = "lena morgan"),
        notStarted(author = "Nora Whitfield"),
      )

    assertEquals(expected = books.take(2), actual = books.byAuthor("LENA MORGAN"))
  }

  @Test
  fun `the books of an author include the whole series they have most books of`() {
    val books =
      listOf(
        notStarted(series = "Saga", author = "Lena Morgan"),
        notStarted(series = "Saga", author = "Lena Morgan"),
        notStarted(series = "Saga", author = "Nora Whitfield"),
        notStarted(series = "Other", author = "Nora Whitfield"),
      )

    assertEquals(expected = books.take(3), actual = books.byAuthor("Lena Morgan"))
    assertEquals(expected = books.drop(2), actual = books.byAuthor("Nora Whitfield"))
  }

  @Test
  fun `no author and a placeholder both find the books with no author`() {
    val books = listOf(notStarted(author = null), notStarted(author = "Unknown"), notStarted())

    assertEquals(expected = books.take(2), actual = books.byAuthor(null))
    assertEquals(expected = books.take(2), actual = books.byAuthor("<unknown>"))
  }

  private fun groups(vararg books: Book): Map<BookCategory, List<AuthorGroup>> =
    libraryEntries(books.toList()).groupedByAuthor()

  private fun notStarted(
    series: String? = null,
    name: String = "Book",
    author: String? = "Author",
    cover: String? = null,
  ): Book = book(series = series, name = name, author = author, time = 0).withCover(cover)

  private fun inProgress(author: String?): Book = book(author = author, time = 5_000)

  // the default book has two chapters of 10 seconds
  private fun finished(
    author: String?,
    lastPlayedAt: Instant = Instant.EPOCH,
  ): Book = book(author = author, time = 20_000, lastPlayedAt = lastPlayedAt)

  private fun Book.withCover(cover: String?): Book =
    if (cover == null) {
      this
    } else {
      copy(content = content.copy(metadata = content.metadata.copy(cover = File(cover))))
    }
}

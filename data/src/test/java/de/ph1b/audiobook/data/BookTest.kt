package de.ph1b.audiobook.data

import org.junit.Test
import java.util.UUID

class BookTest {

  @Test
  fun globalPositionWhenTimeIs0AndCurrentFileIsFirst() {
    val bookId = UUID.randomUUID()
    val book = BookFactory.create(
      time = 0,
      chapters = listOf(ChapterFactory.create(duration = 12345, bookId = bookId)),
      currentFileIndex = 0,
      id = bookId
    )
    book.assertThat().positionIs(0)
  }

  @Test
  fun globalPositionWhenTimeIsNot0AndCurrentFileIsFirst() {
    val bookId = UUID.randomUUID()
    val book = BookFactory.create(
      time = 23,
      chapters = listOf(ChapterFactory.create(duration = 12345, bookId = bookId)),
      currentFileIndex = 0,
      id = bookId
    )
    book.assertThat().positionIs(23)
  }

  @Test
  fun globalPositionWhenTimeIs0AndCurrentFileIsNotFirst() {
    val bookId = UUID.randomUUID()
    val book = BookFactory.create(
      time = 0,
      chapters = listOf(
        ChapterFactory.create(file = "ch1", duration = 123, bookId = bookId),
        ChapterFactory.create(file = "ch2", duration = 234, bookId = bookId),
        ChapterFactory.create(file = "ch3", duration = 345, bookId = bookId),
        ChapterFactory.create(file = "ch4", duration = 456, bookId = bookId)
      ),
      currentFileIndex = 3,
      id = bookId
    )
    book.assertThat().positionIs(123 + 234 + 345)
  }

  @Test
  fun globalPositionWhenTimeIsNot0AndCurrentFileIsNotFirst() {
    val bookId = UUID.randomUUID()
    val book = BookFactory.create(
      time = 23,
      chapters = listOf(
        ChapterFactory.create(file = "ch1", duration = 123, bookId = bookId),
        ChapterFactory.create(file = "ch2", duration = 234, bookId = bookId),
        ChapterFactory.create(file = "ch3", duration = 345, bookId = bookId),
        ChapterFactory.create(file = "ch4", duration = 456, bookId = bookId)
      ),
      currentFileIndex = 2,
      id = bookId
    )
    book.assertThat().positionIs(123 + 234 + 23)
  }

  @Test
  fun totalDuration() {
    val bookId = UUID.randomUUID()
    val book = BookFactory.create(
      chapters = listOf(
        ChapterFactory.create(file = "ch1", duration = 123, bookId = bookId),
        ChapterFactory.create(file = "ch2", duration = 234, bookId = bookId),
        ChapterFactory.create(file = "ch3", duration = 345, bookId = bookId),
        ChapterFactory.create(file = "ch4", duration = 456, bookId = bookId)
      ),
      id = bookId
    )

    book.assertThat().durationIs(123 + 234 + 345 + 456)
  }

  @Test
  fun currentChapter() {
    val bookId = UUID.randomUUID()
    val ch1 = ChapterFactory.create(file = "ch1", bookId = bookId)
    val ch2 = ChapterFactory.create(file = "ch2", bookId = bookId)
    val ch3 = ChapterFactory.create(file = "ch3", bookId = bookId)
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 1,
      id = bookId
    )
    book.assertThat().currentChapterIs(ch2)
  }

  @Test
  fun currentChapterIndex() {
    val bookId = UUID.randomUUID()
    val ch1 = ChapterFactory.create(file = "ch1", bookId = bookId)
    val ch2 = ChapterFactory.create(file = "ch2", bookId = bookId)
    val ch3 = ChapterFactory.create(file = "ch3", bookId = bookId)
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 1,
      id = bookId
    )

    book.assertThat().currentChapterIndexIs(1)
  }

  @Test
  fun nextChapterOnNonLastChapter() {
    val bookId = UUID.randomUUID()
    val ch1 = ChapterFactory.create(file = "ch1", bookId = bookId)
    val ch2 = ChapterFactory.create(file = "ch2", bookId = bookId)
    val ch3 = ChapterFactory.create(file = "ch3", bookId = bookId)
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 1,
      id = bookId
    )

    book.assertThat().nextChapterIs(ch3)
  }

  @Test
  fun nextChapterOnLastChapter() {
    val bookId = UUID.randomUUID()
    val ch1 = ChapterFactory.create(file = "ch1", bookId = bookId)
    val ch2 = ChapterFactory.create(file = "ch2", bookId = bookId)
    val ch3 = ChapterFactory.create(file = "ch3", bookId = bookId)
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 2,
      id = bookId
    )

    book.assertThat().nextChapterIs(null)
  }

  @Test
  fun previousChapterOnNonFirstChapter() {
    val bookId = UUID.randomUUID()
    val ch1 = ChapterFactory.create(file = "ch1", bookId = bookId)
    val ch2 = ChapterFactory.create(file = "ch2", bookId = bookId)
    val ch3 = ChapterFactory.create(file = "ch3", bookId = bookId)
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 1,
      id = bookId
    )
    book.assertThat().previousChapterIs(ch1)
  }

  @Test
  fun previousChapterOnFirstChapter() {
    val bookId = UUID.randomUUID()
    val ch1 = ChapterFactory.create(file = "ch1", bookId = bookId)
    val ch2 = ChapterFactory.create(file = "ch2", bookId = bookId)
    val ch3 = ChapterFactory.create(file = "ch3", bookId = bookId)
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 0,
      id = bookId
    )
    book.assertThat().previousChapterIs(null)
  }
}

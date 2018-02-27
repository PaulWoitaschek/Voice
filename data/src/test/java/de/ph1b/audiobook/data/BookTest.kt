package de.ph1b.audiobook.data

import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.ChapterFactory
import org.junit.Test

class BookTest {

  @Test
  fun globalPositionWhenTimeIs0AndCurrentFileIsFirst() {
    val book = BookFactory.create(
      time = 0,
      chapters = listOf(ChapterFactory.create(duration = 12345)),
      currentFileIndex = 0
    )
    book.assertThat().positionIs(0)
  }

  @Test
  fun globalPositionWhenTimeIsNot0AndCurrentFileIsFirst() {
    val book = BookFactory.create(
      time = 23,
      chapters = listOf(ChapterFactory.create(duration = 12345)),
      currentFileIndex = 0
    )
    book.assertThat().positionIs(23)
  }

  @Test
  fun globalPositionWhenTimeIs0AndCurrentFileIsNotFirst() {
    val book = BookFactory.create(
      time = 0,
      chapters = listOf(
        ChapterFactory.create(file = "ch1", duration = 123),
        ChapterFactory.create(file = "ch2", duration = 234),
        ChapterFactory.create(file = "ch3", duration = 345),
        ChapterFactory.create(file = "ch4", duration = 456)
      ),
      currentFileIndex = 3
    )
    book.assertThat().positionIs(123 + 234 + 345)
  }

  @Test
  fun globalPositionWhenTimeIsNot0AndCurrentFileIsNotFirst() {
    val book = BookFactory.create(
      time = 23,
      chapters = listOf(
        ChapterFactory.create(file = "ch1", duration = 123),
        ChapterFactory.create(file = "ch2", duration = 234),
        ChapterFactory.create(file = "ch3", duration = 345),
        ChapterFactory.create(file = "ch4", duration = 456)
      ),
      currentFileIndex = 2
    )
    book.assertThat().positionIs(123 + 234 + 23)
  }

  @Test
  fun totalDuration() {
    val book = BookFactory.create(
      chapters = listOf(
        ChapterFactory.create(file = "ch1", duration = 123),
        ChapterFactory.create(file = "ch2", duration = 234),
        ChapterFactory.create(file = "ch3", duration = 345),
        ChapterFactory.create(file = "ch4", duration = 456)
      )
    )

    book.assertThat().durationIs(123 + 234 + 345 + 456)
  }

  @Test
  fun currentChapter() {
    val ch1 = ChapterFactory.create(file = "ch1")
    val ch2 = ChapterFactory.create(file = "ch2")
    val ch3 = ChapterFactory.create(file = "ch3")
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 1
    )
    book.assertThat().currentChapterIs(ch2)
  }

  @Test
  fun currentChapterIndex() {
    val ch1 = ChapterFactory.create(file = "ch1")
    val ch2 = ChapterFactory.create(file = "ch2")
    val ch3 = ChapterFactory.create(file = "ch3")
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 1
    )

    book.assertThat().currentChapterIndexIs(1)
  }

  @Test
  fun nextChapterOnNonLastChapter() {
    val ch1 = ChapterFactory.create(file = "ch1")
    val ch2 = ChapterFactory.create(file = "ch2")
    val ch3 = ChapterFactory.create(file = "ch3")
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 1
    )

    book.assertThat().nextChapterIs(ch3)
  }

  @Test
  fun nextChapterOnLastChapter() {
    val ch1 = ChapterFactory.create(file = "ch1")
    val ch2 = ChapterFactory.create(file = "ch2")
    val ch3 = ChapterFactory.create(file = "ch3")
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 2
    )

    book.assertThat().nextChapterIs(null)
  }

  @Test
  fun previousChapterOnNonFirstChapter() {
    val ch1 = ChapterFactory.create(file = "ch1")
    val ch2 = ChapterFactory.create(file = "ch2")
    val ch3 = ChapterFactory.create(file = "ch3")
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 1
    )
    book.assertThat().previousChapterIs(ch1)
  }

  @Test
  fun previousChapterOnFirstChapter() {
    val ch1 = ChapterFactory.create(file = "ch1")
    val ch2 = ChapterFactory.create(file = "ch2")
    val ch3 = ChapterFactory.create(file = "ch3")
    val book = BookFactory.create(
      chapters = listOf(ch1, ch2, ch3),
      currentFileIndex = 0
    )
    book.assertThat().previousChapterIs(null)
  }
}

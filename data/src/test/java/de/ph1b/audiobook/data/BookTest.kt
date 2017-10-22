package de.ph1b.audiobook.data

import com.google.common.truth.Truth.assertThat
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
    assertThat(book.globalPosition).isEqualTo(0)
  }

  @Test
  fun globalPositionWhenTimeIsNot0AndCurrentFileIsFirst() {
    val book = BookFactory.create(
        time = 23,
        chapters = listOf(ChapterFactory.create(duration = 12345)),
        currentFileIndex = 0
    )
    assertThat(book.globalPosition).isEqualTo(23)
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
    assertThat(book.globalPosition).isEqualTo(123 + 234 + 345)
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
    assertThat(book.globalPosition).isEqualTo(123 + 234 + 23)
  }
}

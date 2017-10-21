package de.ph1b.audiobook.data

import de.ph1b.audiobook.BookFactory.chapter
import de.ph1b.audiobook.BookFactory.newBook
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BookTest {

  @Test
  fun testGlobalPositionWhenTimeIs0AndCurrentFileIsFirst() {
    val book = newBook(
        time = 0,
        chapters = listOf(chapter(duration = 12345)),
        currentFileIndex = 0
    )
    assertThat(book.globalPosition()).isEqualTo(0)
  }

  @Test
  fun testGlobalPositionWhenTimeIsNot0AndCurrentFileIsFirst() {
    val book = newBook(
        time = 23,
        chapters = listOf(chapter(duration = 12345)),
        currentFileIndex = 0
    )
    assertThat(book.globalPosition()).isEqualTo(23)
  }

  @Test
  fun testGlobalPositionWhenTimeIs0AndCurrentFileIsNotFirst() {
    val book = newBook(
        time = 0,
        chapters = listOf(
            chapter(file = "ch1", duration = 123),
            chapter(file = "ch2", duration = 234),
            chapter(file = "ch3", duration = 345),
            chapter(file = "ch4", duration = 456)
        ),
        currentFileIndex = 3
    )
    assertThat(book.globalPosition()).isEqualTo(123 + 234 + 345)
  }

  @Test
  fun testGlobalPositionWhenTimeIsNot0AndCurrentFileIsNotFirst() {
    val book = newBook(
        time = 23,
        chapters = listOf(
            chapter(file = "ch1", duration = 123),
            chapter(file = "ch2", duration = 234),
            chapter(file = "ch3", duration = 345),
            chapter(file = "ch4", duration = 456)
        ),
        currentFileIndex = 2
    )
    assertThat(book.globalPosition()).isEqualTo(123 + 234 + 23)
  }
}

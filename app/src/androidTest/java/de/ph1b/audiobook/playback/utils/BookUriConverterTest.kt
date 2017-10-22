package de.ph1b.audiobook.playback.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test


class BookUriConverterTest {

  private lateinit var converter: BookUriConverter

  @Before
  fun setUp() {
    converter = BookUriConverter()
  }

  @Test
  fun allBooksType() {
    val uri = converter.allBooks()
    val match = converter.type(uri)
    assertThat(match).isEqualTo(BookUriConverter.ROOT)
  }

  @Test
  fun singleBookType() {
    val uri = converter.book(1)
    val match = converter.type(uri)
    assertThat(match).isEqualTo(BookUriConverter.BOOK_ID)
  }

  @Test
  fun chapterType() {
    val uri = converter.chapter(5, 9)
    println(uri)
    val match = converter.type(uri)
    assertThat(match).isEqualTo(BookUriConverter.CHAPTER_ID)
  }

  @Test
  fun extractBookOnly() {
    val bookId = 153L
    val uri = converter.book(bookId)
    val extracted = converter.extractBook(uri)
    assertThat(extracted).isEqualTo(bookId)
  }
}


package de.ph1b.audiobook.playback.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.playback.session.BookUriConverter
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class BookUriConverterTest {

  private val converter = BookUriConverter()

  @Test
  fun allBooksType() {
    val allBooksId = converter.allBooksId()
    val parsed = converter.parse(allBooksId)
    assertThat(parsed).isEqualTo(BookUriConverter.Parsed.AllBooks)
  }

  @Test
  fun book() {
    val bookId = Book2.Id(UUID.randomUUID().toString())
    val id = converter.bookId(bookId)
    val parsed = converter.parse(id)
    assertThat(parsed).isEqualTo(BookUriConverter.Parsed.Book(bookId))
  }
}

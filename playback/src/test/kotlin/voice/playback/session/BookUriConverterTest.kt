package voice.playback.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.common.BookId
import voice.data.Chapter
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class BookUriConverterTest {

  private val converter = BookUriConverter()

  @Test
  fun allBooksType() {
    val allBooksId = converter.allBooksId()
    val parsed = converter.parse(allBooksId)
    parsed shouldBe BookUriConverter.Parsed.AllBooks
  }

  @Test
  fun book() {
    val bookId = BookId(UUID.randomUUID().toString())
    val id = converter.book(bookId)
    val parsed = converter.parse(id)
    parsed shouldBe BookUriConverter.Parsed.Book(bookId)
  }

  @Test
  fun chapter() {
    val bookId = BookId(UUID.randomUUID().toString())
    val chapterId = Chapter.Id(UUID.randomUUID().toString())
    val id = converter.chapter(bookId, chapterId)
    val parsed = converter.parse(id)
    parsed shouldBe BookUriConverter.Parsed.Chapter(bookId, chapterId)
  }
}

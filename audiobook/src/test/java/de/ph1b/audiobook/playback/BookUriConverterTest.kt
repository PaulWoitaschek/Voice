package de.ph1b.audiobook.playback

import android.os.Build
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.TestApp
import de.ph1b.audiobook.playback.utils.BookUriConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Test for the book uri spec
 *
 * @author Paul Woitaschek
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), manifest = "src/main/AndroidManifest.xml", application = TestApp::class)
class BookUriConverterTest {

  lateinit var converter: BookUriConverter

  init {
    ShadowLog.stream = System.out
  }

  @Before fun setUp() {
    converter = BookUriConverter()
  }

  @Test fun testAllBooks() {
    val uri = converter.allBooks()

    println(uri)

    val match = converter.match(uri)

    assertThat(match).isEqualTo(BookUriConverter.ROOT)
  }

  @Test fun testSingleBook() {
    val uri = converter.book(1)

    val match = converter.match(uri)

    assertThat(match).isEqualTo(BookUriConverter.BOOK_ID)
  }

  @Test fun testExtractBookOnly() {
    val bookId = 153L

    val uri = converter.book(bookId)

    val extracted = converter.extractBook(uri)

    assertThat(bookId).isEqualTo(extracted)
  }
}
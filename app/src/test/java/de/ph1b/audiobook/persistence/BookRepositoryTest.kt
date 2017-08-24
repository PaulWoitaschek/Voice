package de.ph1b.audiobook.persistence

import android.os.Build
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.TestApp
import de.ph1b.audiobook.persistence.internals.BookStorage
import de.ph1b.audiobook.persistence.internals.InternalDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Test for the book repository.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP),
    manifest = "src/main/AndroidManifest.xml",
    application = TestApp::class
)
class BookRepositoryTest {

  init {
    ShadowLog.stream = System.out
  }

  private lateinit var repo: BookRepository

  @Before
  fun setUp() {
    val internalDb = InternalDb(RuntimeEnvironment.application)
    val moshi = Moshi.Builder().build()
    val internalBookRegister = BookStorage(internalDb, moshi)
    repo = BookRepository(internalBookRegister)
  }

  @Test
  fun testInOut() {
    val dummy = BookFactory.create()
    repo.addBook(dummy)
    val firstBook = repo.activeBooks.first()
    val dummyWithUpdatedId = dummy.copy(id = firstBook.id)

    assertThat(dummyWithUpdatedId).isEqualTo(firstBook)
  }
}

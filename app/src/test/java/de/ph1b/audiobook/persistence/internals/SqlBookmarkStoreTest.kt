package de.ph1b.audiobook.persistence.internals

import android.os.Build
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.TestApp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Test for the internal bookmark register
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP),
    manifest = "src/main/AndroidManifest.xml",
    application = TestApp::class
)
class SqlBookmarkStoreTest {

  lateinit var register: SqlBookmarkStore

  @Before
  fun setup() {
    val internalDb = InternalDb(RuntimeEnvironment.application)
    register = SqlBookmarkStore(internalDb)
  }

  @Test
  fun test() {
    // test adding
    val book = BookFactory.create()
    val added = (0..2000).map {
      register.addBookmark(Bookmark(book.chapters.first().file, "my title", System.currentTimeMillis().toInt()))
    }

    // test inserted match
    val bookmarks = register.bookmarks(book)
    assertThat(bookmarks).isEqualTo(added)

    // delete a part
    val toDelete = bookmarks.subList(0, 5)
    val notToDelete = bookmarks.subList(5, bookmarks.size)
    toDelete.forEach {
      register.deleteBookmark(it.id)
    }

    // check that only the non deleted remain
    assertThat(register.bookmarks(book)).isEqualTo(notToDelete)
  }
}

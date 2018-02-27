package de.ph1b.audiobook.data.repo.internals

import android.support.test.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.repo.ClearDbRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class SqlBookmarkStoreTest {

  private lateinit var register: SqlBookmarkStore

  @Rule
  @JvmField
  val clearAppDbRule = ClearDbRule()

  @Before
  fun setup() {
    val context = InstrumentationRegistry.getTargetContext()
    val internalDb = InternalDb(context)
    register = SqlBookmarkStore(internalDb)
  }

  @Test
  fun test() {
    // test adding
    val book = BookFactory.create()
    val added = (0..2000).map {
      register.addBookmark(
        Bookmark(
          book.chapters.first().file,
          "my title",
          System.currentTimeMillis().toInt()
        )
      )
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

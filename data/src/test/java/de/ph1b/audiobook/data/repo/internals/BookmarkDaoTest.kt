package de.ph1b.audiobook.data.repo.internals

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookFactory
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.di.DataComponent
import de.ph1b.audiobook.data.di.DataInjector
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {

  init {
    DataInjector.component = object : DataComponent {
      override fun inject(converters: Converters) {
        converters.moshi = Moshi.Builder().build()
      }
    }
  }

  private val dao =
    Room.inMemoryDatabaseBuilder(getApplicationContext(), AppDb::class.java)
      .allowMainThreadQueries()
      .build()
      .bookmarkDao()

  @Test
  fun test() {
    // test adding
    val book = BookFactory.create()
    val added = (0..10)
      .map {
        Bookmark(
          book.content.chapters.first().file,
          "my title",
          System.currentTimeMillis().toInt()
        )
      }
      .map {
        val addedId = dao.addBookmark(it)
        it.copy(id = addedId)
      }

    // test inserted match
    val bookmarks = bookmarksForBook(book)
    assertThat(bookmarks).isEqualTo(added)

    // delete a part
    val toDelete = bookmarks.subList(0, 5)
    val notToDelete = bookmarks.subList(5, bookmarks.size)
    toDelete.forEach {
      dao.deleteBookmark(it.id)
    }

    // check that only the non deleted remain
    assertThat(bookmarksForBook(book)).isEqualTo(notToDelete)
  }

  private fun bookmarksForBook(book: Book) = dao.allForFiles(book.content.chapters.map { it.file })
}

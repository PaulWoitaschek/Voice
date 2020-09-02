package de.ph1b.audiobook.data.repo.internals

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookFactory
import de.ph1b.audiobook.data.Bookmark
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class BookmarkDaoTest {

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
          System.currentTimeMillis(),
          addedAt = Instant.now(),
          setBySleepTimer = false,
          id = UUID.randomUUID()
        )
      }
      .onEach {
        runBlocking {
          dao.addBookmark(it)
        }
      }

    // test inserted match
    val bookmarks = bookmarksForBook(book)
    assertThat(bookmarks).isEqualTo(added)

    // delete a part
    val toDelete = bookmarks.subList(0, 5)
    val notToDelete = bookmarks.subList(5, bookmarks.size)
    toDelete.forEach {
      runBlocking {
        dao.deleteBookmark(it.id)
      }
    }

    // check that only the non deleted remain
    assertThat(bookmarksForBook(book)).isEqualTo(notToDelete)
  }

  private fun bookmarksForBook(book: Book): List<Bookmark> {
    return runBlocking {
      dao.allForFiles(book.content.chapters.map { it.file })
    }
  }
}

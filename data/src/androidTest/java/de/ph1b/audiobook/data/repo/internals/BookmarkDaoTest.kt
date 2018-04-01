package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Bookmark
import org.junit.Test


class BookmarkDaoTest {

  private val dao =
    Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getTargetContext(), AppDb::class.java)
      .build()
      .bookmarkDao()

  @Test
  fun test() {
    // test adding
    val book = BookFactory.create()
    val added = (0..10).map {
      Bookmark(
        book.content.chapters.first().file,
        "my title",
        System.currentTimeMillis().toInt()
      ).let {
        val id = dao.addBookmark(it)
        it.copy(id = id)
      }
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

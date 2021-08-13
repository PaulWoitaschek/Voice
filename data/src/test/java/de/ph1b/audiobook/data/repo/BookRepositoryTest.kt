package de.ph1b.audiobook.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookFactory
import de.ph1b.audiobook.data.repo.internals.AppDb
import de.ph1b.audiobook.data.repo.internals.BookStorage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class BookRepositoryTest {

  private val storage: BookStorage

  init {
    val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .allowMainThreadQueries()
      .build()
    storage = BookStorage(db.chapterDao(), db.bookMetadataDao(), db.bookSettingsDao(), db)
  }

  @Test
  fun booksSameInMemoryAndStorage() {
    runBlocking {
      val repo = BookRepository(storage)
      repo.addBook(BookFactory.create())
      assertThat(storage.books())
        .comparingElementsUsing(ignoringChapterIds())
        .containsExactlyElementsIn(repo.allBooks())
        .inOrder()
    }
  }

  @Test
  fun updateBookName() {
    runBlocking {
      val bookId = UUID.randomUUID()
      val repo = BookRepository(storage)
      repo.addBook(BookFactory.create(id = bookId))
      repo.updateBookName(bookId, "Dark")
      assertThat(storage.books())
        .comparingElementsUsing(ignoringChapterIds())
        .containsExactlyElementsIn(repo.allBooks())
        .inOrder()
    }
  }

  private fun ignoringChapterIds(): Correspondence<Book, Book> {
    return Correspondence.from(
      { actual, expected ->
        actual?.withZeroedChapterIds() == expected?.withZeroedChapterIds()
      },
      "compare"
    )
  }
}

private fun Book.withZeroedChapterIds(): Book = updateContent {
  copy(
    chapters = chapters.map { chapter ->
      chapter.copy(id = 0)
    }
  )
}

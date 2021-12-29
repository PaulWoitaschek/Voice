package de.ph1b.audiobook.data.repo.internals

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookFactory
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class BookStorageTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val storage = BookStorage(db.chapterDao(), db.bookMetadataDao(), db.bookSettingsDao(), db)

  private val bookA: Book
  private val bookB: Book

  init {
    val bookAId = UUID.randomUUID()
    val bookBId = UUID.randomUUID()
    val books = runBlocking {
      storage.addOrUpdate(BookFactory.create(name = "Name A", lastPlayedAtMillis = 5, id = bookAId))
      storage.addOrUpdate(BookFactory.create(name = "Name B", lastPlayedAtMillis = 10, id = bookBId))
      storage.books()
    }

    bookA = books.single { it.id == bookAId }
    bookB = books.single { it.id == bookBId }
  }

  @Test
  fun updateName() {
    runBlocking {
      storage.updateBookName(bookA.id, "Name A2")
      val books = storage.books()
      val updatedBook = bookA.updateMetaData { copy(name = "Name A2") }
      assertThat(books).containsExactly(updatedBook, bookB)
    }
  }

  @Test
  fun updateLastPlayedAt() {
    runBlocking {
      storage.updateLastPlayedAt(bookA.id, 500)
      val books = storage.books()
      val updatedBook = bookA.update(updateSettings = { copy(lastPlayedAtMillis = 500) })
      assertThat(books).containsExactly(updatedBook, bookB)
    }
  }
}

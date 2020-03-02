package de.ph1b.audiobook.data.repo.internals

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.data.BookFactory
import de.ph1b.audiobook.data.di.DataComponent
import de.ph1b.audiobook.data.di.DataInjector
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class BookStorageTest {

  init {
    DataInjector.component = object : DataComponent {
      override fun inject(converters: Converters) {
        converters.moshi = Moshi.Builder().build()
      }
    }
  }

  private val db =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .allowMainThreadQueries()
      .build()
  private val storage = BookStorage(db.chapterDao(), db.bookMetadataDao(), db.bookSettingsDao(), db)

  val bookA = BookFactory.create(name = "Name A", lastPlayedAtMillis = 5)
  val bookB = BookFactory.create(name = "Name B", lastPlayedAtMillis = 10)

  init {
    runBlocking {
      storage.addOrUpdate(bookA)
      storage.addOrUpdate(bookB)
    }
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

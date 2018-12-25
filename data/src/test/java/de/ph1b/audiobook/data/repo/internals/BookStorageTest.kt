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

@RunWith(AndroidJUnit4::class)
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

  @Test
  fun updateName() {
    runBlocking {
      val bookA = BookFactory.create(name = "Name A")
      storage.addOrUpdate(bookA)
      val bookB = BookFactory.create(name = "Name B")
      storage.addOrUpdate(bookB)
      storage.updateBookName(bookA.id, "Name A2")
      val books = storage.books()
      assertThat(books).containsExactly(bookA.updateMetaData { copy(name = "Name A2") }, bookB)
    }
  }
}

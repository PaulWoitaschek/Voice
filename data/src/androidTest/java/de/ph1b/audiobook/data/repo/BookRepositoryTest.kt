package de.ph1b.audiobook.data.repo

import androidx.room.Room
import androidx.test.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.data.repo.internals.AppDb
import de.ph1b.audiobook.data.repo.internals.BookStorage
import de.ph1b.audiobook.data.repo.internals.PersistenceModule
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BookRepositoryTest {

  private lateinit var repo: BookRepository

  @Rule
  @JvmField
  val clearAppDbRule = ClearDbRule()

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getTargetContext()
    val helper = PersistenceModule()
      .appDb(
        builder = Room.inMemoryDatabaseBuilder(context, AppDb::class.java),
        callback = InitialRoomCallback(),
        migrations = PersistenceModule().migrations(context)
      )
      .openHelper
    val moshi = Moshi.Builder().build()
    val internalBookRegister = BookStorage(helper, moshi)
    repo = BookRepository(internalBookRegister)
  }

  @Test
  fun inOut() {
    runBlocking {
      val dummy = BookFactory.create()
      repo.addBook(dummy)
      val firstBook = repo.activeBooks.first()
      val dummyWithUpdatedId = dummy.copy(
        id = firstBook.id, content = dummy.content.copy(
          id = firstBook.id
        )
      )
      assertThat(dummyWithUpdatedId).isEqualTo(firstBook)
    }
  }
}

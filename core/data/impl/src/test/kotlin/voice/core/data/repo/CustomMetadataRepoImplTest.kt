package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.CustomMetadata
import voice.core.data.repo.internals.AppDb
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class CustomMetadataRepoImplTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val repo = CustomMetadataRepoImpl(db.customMetadataDao())

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `edits are read back`() = runTest {
    val custom = CustomMetadata(BookId("dune"), name = "Dune", author = "Frank Herbert", cover = File("/covers/dune.webp"))
    repo.put(custom)

    assertEquals(custom, CustomMetadataRepoImpl(db.customMetadataDao()).get(custom.bookId))
  }

  @Test
  fun `a book without edits has no row`() = runTest {
    val id = BookId("dune")
    repo.put(CustomMetadata(id, name = "Dune"))
    repo.put(CustomMetadata(id))

    assertEquals(CustomMetadata(id), repo.get(id))
    assertEquals(emptyList(), db.customMetadataDao().all())
  }
}

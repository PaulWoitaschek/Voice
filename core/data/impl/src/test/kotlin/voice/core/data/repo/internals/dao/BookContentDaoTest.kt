package voice.core.data.repo.internals.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.LargeCursorWindowOpenHelperFactory
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BookContentDaoTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .openHelperFactory(LargeCursorWindowOpenHelperFactory())
    .build()
  private val dao = db.bookContentDao()

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `a book with more chapters than fit into the default cursor window can be read`() = runTest {
    val folder = "content://com.android.externalstorage.documents/tree/primary%3AAudiobooks" +
      "/document/primary%3AAudiobooks%2FA%20very%20long%20book%20with%20a%20lot%20of%20files"
    val chapters = List(15_000) { ChapterId("$folder%2FChapter%20${it.toString().padStart(5, '0')}.mp3") }
    val book = BookContent(
      id = BookId(folder),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      author = null,
      name = "A very long book",
      addedAt = Instant.EPOCH,
      chapters = chapters,
      currentChapter = chapters.last(),
      positionInChapter = 0,
      cover = null,
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
    )
    dao.insert(book)

    assertEquals(expected = listOf(book), actual = dao.all())
  }
}

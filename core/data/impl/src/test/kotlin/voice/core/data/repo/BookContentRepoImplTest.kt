package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.ChapterId
import voice.core.data.MAX_CHAPTER_IDS_BYTES
import voice.core.data.fitIntoOneBook
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.Converters
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class BookContentRepoImplTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val repo = BookContentRepoImpl(db.bookContentDao())

  @After
  fun tearDown() {
    db.close()
  }

  private fun content(
    name: String,
    chapters: List<ChapterId> = listOf(ChapterId("chapter")),
  ) = BookContent(
    id = BookId(name),
    playbackSpeed = 1F,
    skipSilence = false,
    isActive = true,
    lastPlayedAt = Instant.EPOCH,
    addedAt = Instant.EPOCH,
    chapters = chapters,
    currentChapter = chapters.first(),
    positionInChapter = 0,
    gain = 0F,
    metadata = BookMetadata(
      name = name,
      author = null,
      album = null,
      title = null,
      genre = null,
      narrator = null,
      series = null,
      part = null,
      cover = null,
      fileLastModified = null,
    ),
  )

  @Test
  fun `a book with as many chapters as fit into one book can be read back`() = runTest {
    val folder = "content://com.android.externalstorage.documents/tree/primary%3AAudiobooks" +
      "/document/primary%3AAudiobooks%2FA%20very%20long%20book"
    val chapterId = { index: Int -> ChapterId("$folder%2FChapter%20${index.toString().padStart(5, '0')}.mp3") }
    val idBytes = chapterId(0).value.length + 3
    val chapters = List((MAX_CHAPTER_IDS_BYTES - 1) / idBytes, chapterId)
    assertTrue(chapters.fitIntoOneBook())
    assertFalse((chapters + chapterId(chapters.size)).fitIntoOneBook())
    assertTrue(Converters().fromChapterList(chapters).length > MAX_CHAPTER_IDS_BYTES - idBytes)

    val book = content(name = "A very long book", chapters = chapters)
    repo.put(book)

    assertEquals(listOf(book), BookContentRepoImpl(db.bookContentDao()).all())
  }
}

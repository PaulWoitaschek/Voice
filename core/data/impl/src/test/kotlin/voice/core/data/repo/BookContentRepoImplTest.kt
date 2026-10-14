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
    author: String?,
    isActive: Boolean = true,
    chapters: List<ChapterId> = listOf(ChapterId("chapter")),
  ) = BookContent(
    id = BookId(name),
    playbackSpeed = 1F,
    skipSilence = false,
    isActive = isActive,
    lastPlayedAt = Instant.EPOCH,
    addedAt = Instant.EPOCH,
    chapters = chapters,
    currentChapter = chapters.first(),
    positionInChapter = 0,
    gain = 0F,
    metadata = BookMetadata(
      name = name,
      author = author,
      album = null,
      title = null,
      genre = null,
      narrator = null,
      series = null,
      part = null,
      cover = null,
      fileLastModified = null,
    ),
    customName = null,
    customCover = null,
  )

  @Test
  fun `search finds the active books with a word starting with each prefix`() = runTest {
    val wind = content(name = "The Name of the Wind", author = "Patrick Rothfuss")
    val shining = content(name = "The Shining", author = "Stephen King")
    repo.put(wind)
    repo.put(shining)
    repo.put(content(name = "The Wise Man's Fear", author = "Patrick Rothfuss", isActive = false))

    assertEquals(listOf(wind.id), repo.search(listOf("roth")))
    assertEquals(listOf(wind.id), repo.search(listOf("pat", "wi")))
    assertEquals(emptyList(), repo.search(listOf("roth", "king")))
    assertEquals(listOf(wind.id, shining.id), repo.search(listOf("the")).sortedBy { it.value })
  }

  @Test
  fun `search finds a book by the name the listener gave it and the name from its files`() = runTest {
    val book = content(name = "Track 01", author = null).withCustomName("Dune")
    repo.put(book)

    assertEquals(listOf(book.id), repo.search(listOf("dune")))
    assertEquals(listOf(book.id), repo.search(listOf("track")))
  }

  @Test
  fun `search reads prefixes as text, not as query syntax`() = runTest {
    val shining = content(name = "The Shining", author = "Stephen King")
    repo.put(shining)

    assertEquals(listOf(shining.id), repo.search(listOf("king\"", "(shin")))
    assertEquals(emptyList(), repo.search(listOf("king", "OR", "carrie")))
    assertEquals(emptyList(), repo.search(listOf("\"", "*", "-")))
    assertEquals(emptyList(), repo.search(emptyList()))
  }

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

    val book = content(name = "A very long book", author = null, chapters = chapters)
    repo.put(book)

    assertEquals(listOf(book), BookContentRepoImpl(db.bookContentDao()).all())
  }
}

package voice.core.data.repo

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
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

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
  ) = BookContent(
    id = BookId(name),
    playbackSpeed = 1F,
    skipSilence = false,
    isActive = isActive,
    lastPlayedAt = Instant.EPOCH,
    author = author,
    name = name,
    addedAt = Instant.EPOCH,
    chapters = listOf(ChapterId("chapter")),
    currentChapter = ChapterId("chapter"),
    positionInChapter = 0,
    cover = null,
    gain = 0F,
    genre = null,
    narrator = null,
    series = null,
    part = null,
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
  fun `search reads prefixes as text, not as query syntax`() = runTest {
    val shining = content(name = "The Shining", author = "Stephen King")
    repo.put(shining)

    assertEquals(listOf(shining.id), repo.search(listOf("king\"", "(shin")))
    assertEquals(emptyList(), repo.search(listOf("king", "OR", "carrie")))
    assertEquals(emptyList(), repo.search(listOf("\"", "*", "-")))
    assertEquals(emptyList(), repo.search(emptyList()))
  }
}

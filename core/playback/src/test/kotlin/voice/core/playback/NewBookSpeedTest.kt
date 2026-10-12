package voice.core.playback

import kotlinx.coroutines.test.runTest
import voice.core.data.Book
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.playback.session.search.book
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class NewBookSpeedTest {

  private val newBook = book(
    chapters = listOf(
      Chapter(
        id = ChapterId("chapter"),
        name = "Chapter",
        duration = 10_000,
        fileLastModified = Instant.EPOCH,
        markData = emptyList(),
        fileSize = 0,
      ),
    ),
  )

  @Test
  fun `a new book starts at the speed played at last`() = runTest {
    assertEquals(expected = 1.75F, actual = speedAfterApplying(newBook, lastSpeed = 1.75F))
  }

  @Test
  fun `a book that was played keeps its speed`() = runTest {
    val played = newBook.update { it.copy(lastPlayedAt = Instant.ofEpochSecond(1)) }

    assertEquals(expected = 1F, actual = speedAfterApplying(played, lastSpeed = 1.75F))
  }

  @Test
  fun `a speed picked before the first play stays`() = runTest {
    val picked = newBook.update { it.copy(playbackSpeed = 1.25F) }

    assertEquals(expected = 1.25F, actual = speedAfterApplying(picked, lastSpeed = 1.75F))
  }

  private suspend fun speedAfterApplying(
    book: Book,
    lastSpeed: Float,
  ): Float {
    val repo = MemoryBookRepository(book)
    NewBookSpeed(MemoryDataStore(lastSpeed), repo).applyTo(book.id)
    return repo.get(book.id)!!.content.playbackSpeed
  }
}

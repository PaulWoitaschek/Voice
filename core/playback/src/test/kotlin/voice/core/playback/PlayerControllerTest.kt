package voice.core.playback

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.playback.session.search.book
import java.time.Instant

class PlayerControllerTest {

  private val chapter1 = Chapter(
    id = ChapterId("chapter-1"),
    name = "Chapter 1",
    duration = 10_000,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
  )
  private val chapter2 = Chapter(
    id = ChapterId("chapter-2"),
    name = "Chapter 2",
    duration = 20_000,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
  )
  private val bookA = book(chapters = listOf(chapter1, chapter2))
  private val bookB = book(chapters = listOf(chapter1))

  @Test
  fun `resolveSeekIndex returns chapter index when expectedBookId matches current book`() = runTest {
    val store = MemoryDataStore<BookId?>(bookA.id)
    val repo = mockk<BookRepository> { coEvery { get(bookA.id) } returns bookA }

    resolveSeekIndex(bookA.id, chapter2.id, store, repo) shouldBe 1
  }

  @Test
  fun `resolveSeekIndex returns null when expectedBookId does not match current book`() = runTest {
    val store = MemoryDataStore<BookId?>(bookB.id)
    val repo = mockk<BookRepository>()

    resolveSeekIndex(bookA.id, chapter1.id, store, repo) shouldBe null
  }

  @Test
  fun `resolveSeekIndex returns null when no book is loaded`() = runTest {
    val store = MemoryDataStore<BookId?>(null)
    val repo = mockk<BookRepository>()

    resolveSeekIndex(bookA.id, chapter1.id, store, repo) shouldBe null
  }

  @Test
  fun `resolveSeekIndex returns null when chapter is not found in book`() = runTest {
    val store = MemoryDataStore<BookId?>(bookA.id)
    val repo = mockk<BookRepository> { coEvery { get(bookA.id) } returns bookA }

    resolveSeekIndex(bookA.id, ChapterId("unknown"), store, repo) shouldBe null
  }
}

package voice.core.playback

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.session.search.book
import java.time.Instant

class CurrentBookResolverTest {

  private val book = book(
    chapters = listOf(
      Chapter(
        id = ChapterId("chapter-1"),
        name = "Chapter 1",
        duration = 10_000,
        fileLastModified = Instant.EPOCH,
        markData = emptyList(),
      ),
      Chapter(
        id = ChapterId("chapter-2"),
        name = "Chapter 2",
        duration = 10_000,
        fileLastModified = Instant.EPOCH,
        markData = emptyList(),
      ),
    ),
  )
  private val currentBookStore = MemoryDataStore<BookId?>(book.id)

  @Test
  fun `returns persisted book when live playback persistence is disabled`() = runTest {
    val resolver = CurrentBookResolver(
      bookRepository = mockk {
        coEvery { get(book.id) } returns book
      },
      playerController = mockk(),
      currentBookStore = currentBookStore,
      experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(false),
    )

    resolver.currentBook() shouldBe book
  }

  @Test
  fun `overlays the current player position when live playback persistence is enabled`() = runTest {
    val resolver = CurrentBookResolver(
      bookRepository = mockk {
        coEvery { get(book.id) } returns book
      },
      playerController = mockk {
        coEvery { livePlaybackState(book.id) } returns LivePlaybackState(
          bookId = book.id,
          chapterId = book.chapters.last().id,
          positionMs = 1234L,
          isPlaying = false,
          playbackSpeed = 1f,
        )
      },
      currentBookStore = currentBookStore,
      experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(true),
    )

    resolver.currentBook()?.content?.currentChapter shouldBe book.chapters.last().id
    resolver.currentBook()?.content?.positionInChapter shouldBe 1234L
  }

  @Test
  fun `returns persisted book when no live playback state is available`() = runTest {
    val resolver = CurrentBookResolver(
      bookRepository = mockk {
        coEvery { get(book.id) } returns book
      },
      playerController = mockk {
        coEvery { livePlaybackState(book.id) } returns null
      },
      currentBookStore = currentBookStore,
      experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(true),
    )

    resolver.currentBook() shouldBe book
  }
}

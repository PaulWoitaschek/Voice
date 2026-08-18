package voice.core.playback.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.playback.MemoryDataStore
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.search.book
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class UpNextAdvancerTest {

  private val currentBook = book(chapters = listOf(chapter()))
  private val upNextBook = book(chapters = listOf(chapter(), chapter()))

  private val currentBookStore = MemoryDataStore<BookId?>(currentBook.id)
  private val upNextBookStore = MemoryDataStore<BookId?>(upNextBook.id)

  private val bookRepository = mockk<BookRepository> {
    coEvery { get(upNextBook.id) } returns upNextBook
    coEvery { updateBook(any(), any()) } just Runs
  }
  private val mediaItemProvider = mockk<MediaItemProvider> {
    every { mediaItem(any()) } returns mockk<MediaItem>()
  }
  private val player = mockk<Player>(relaxed = true)
  private val scope = TestScope()

  private val advancer = UpNextAdvancer(
    upNextBookStore = upNextBookStore,
    currentBookStore = currentBookStore,
    bookRepository = bookRepository,
    mediaItemProvider = mediaItemProvider,
    scope = scope,
  ).apply { attachTo(player) }

  @Test
  fun `promotes the up-next book and starts playback when the current book ends`() = scope.runTest {
    advancer.onPlaybackStateChanged(Player.STATE_ENDED)
    runCurrent()

    assertEquals(expected = upNextBook.id, actual = currentBookStore.data.first())
    assertNull(upNextBookStore.data.first())
    verify { player.setMediaItem(any()) }
    verify { player.prepare() }
    verify { player.play() }
  }

  @Test
  fun `marks the up-next book as started at the first chapter`() = scope.runTest {
    advancer.onPlaybackStateChanged(Player.STATE_ENDED)
    runCurrent()

    val update = slot<(BookContent) -> BookContent>()
    coVerify { bookRepository.updateBook(upNextBook.id, capture(update)) }
    val updated = update.captured(upNextBook.content)
    assertEquals(expected = upNextBook.chapters.first().id, actual = updated.currentChapter)
    assertEquals(expected = 1L, actual = updated.positionInChapter)
  }

  @Test
  fun `does nothing when no book is queued`() = scope.runTest {
    upNextBookStore.updateData { null }

    advancer.onPlaybackStateChanged(Player.STATE_ENDED)
    runCurrent()

    assertEquals(expected = currentBook.id, actual = currentBookStore.data.first())
    verify(exactly = 0) { player.setMediaItem(any()) }
    verify(exactly = 0) { player.prepare() }
    verify(exactly = 0) { player.play() }
  }

  @Test
  fun `ignores playback state changes other than ended`() = scope.runTest {
    advancer.onPlaybackStateChanged(Player.STATE_READY)
    runCurrent()

    assertEquals(expected = currentBook.id, actual = currentBookStore.data.first())
    assertEquals(expected = upNextBook.id, actual = upNextBookStore.data.first())
    verify(exactly = 0) { player.setMediaItem(any()) }
  }

  @Test
  fun `clears the queue when the up-next book no longer exists`() = scope.runTest {
    coEvery { bookRepository.get(upNextBook.id) } returns null

    advancer.onPlaybackStateChanged(Player.STATE_ENDED)
    runCurrent()

    assertNull(upNextBookStore.data.first())
    assertEquals(expected = currentBook.id, actual = currentBookStore.data.first())
    verify(exactly = 0) { player.setMediaItem(any()) }
  }

  private fun chapter(): Chapter {
    return Chapter(
      id = ChapterId(Uuid.random().toString()),
      name = "chapter",
      duration = 10_000,
      fileLastModified = Instant.EPOCH,
      markData = emptyList(),
      fileSize = 0,
    )
  }
}

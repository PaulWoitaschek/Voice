package voice.core.playback

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import voice.core.data.BookContent
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.MarkData
import voice.core.data.repo.BookRepository
import voice.core.playback.session.search.book
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class DefaultPlaybackSpeedApplierTest {

  private val store = MemoryDataStore(1.25F)
  private val repo = mockk<BookRepository> {
    coEvery { updateBook(any(), any()) } just Runs
  }
  private val applier = DefaultPlaybackSpeedApplier(store, repo)

  @Test
  fun `new book gets the default and it is persisted`() = runTest {
    val content = content()

    assertEquals(expected = 1.25F, actual = applier.applyTo(content))

    coVerify { repo.updateBook(content.id, any()) }
  }

  @Test
  fun `played book keeps its speed`() = runTest {
    val content = content(lastPlayedAt = Instant.ofEpochMilli(1_000))

    assertEquals(expected = 1F, actual = applier.applyTo(content))

    coVerify(exactly = 0) { repo.updateBook(any(), any()) }
  }

  @Test
  fun `new book with custom speed keeps it`() = runTest {
    val content = content(playbackSpeed = 1.5F)

    assertEquals(expected = 1.5F, actual = applier.applyTo(content))

    coVerify(exactly = 0) { repo.updateBook(any(), any()) }
  }

  @Test
  fun `new book is untouched when the default is 1x`() = runTest {
    store.updateData { 1F }

    assertEquals(expected = 1F, actual = applier.applyTo(content()))

    coVerify(exactly = 0) { repo.updateBook(any(), any()) }
  }

  private fun content(
    playbackSpeed: Float = 1F,
    lastPlayedAt: Instant = Instant.EPOCH,
  ): BookContent {
    return book(chapters = listOf(chapter()))
      .content
      .copy(playbackSpeed = playbackSpeed, lastPlayedAt = lastPlayedAt)
  }

  private fun chapter(): Chapter {
    return Chapter(
      id = ChapterId(Uuid.random().toString()),
      name = "chapter",
      duration = 10_000,
      fileLastModified = Instant.EPOCH,
      markData = listOf(MarkData(0, "mark")),
      fileSize = 0,
    )
  }
}

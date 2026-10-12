package voice.core.playback

import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.playback.session.search.book
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSpeedTest {

  private val book = book(
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
  fun `a book that was never played starts at the default speed`() {
    assertEquals(expected = 1.75F, actual = book.withDefaultSpeed { 1.75F }.content.playbackSpeed)
  }

  @Test
  fun `a book that was played keeps its own speed without reading the default`() {
    val played = book.update { it.copy(playbackSpeed = 1.25F, lastPlayedAt = Instant.ofEpochSecond(1)) }

    val result = played.withDefaultSpeed { error("the default speed is only read for a book that was never played") }

    assertEquals(expected = 1.25F, actual = result.content.playbackSpeed)
  }

  @Test
  fun `a book with a speed of its own but no last play keeps its speed`() {
    val legacy = book.update { it.copy(playbackSpeed = 1.25F) }

    assertEquals(expected = 1.25F, actual = legacy.withDefaultSpeed { 1.75F }.content.playbackSpeed)
  }
}

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
    assertEquals(expected = 1.75F, actual = book.withDefaultSpeed(1.75F).content.playbackSpeed)
  }

  @Test
  fun `a book that was played keeps its own speed`() {
    val played = book.update { it.copy(playbackSpeed = 1.25F, lastPlayedAt = Instant.ofEpochSecond(1)) }

    assertEquals(expected = 1.25F, actual = played.withDefaultSpeed(1.75F).content.playbackSpeed)
  }
}

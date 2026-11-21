package voice.core.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.playback.session.search.book

class LivePlaybackStateTest {

  private val book = book(chapters = listOf(chapter("chapter-1"), chapter("chapter-2")))

  @Test
  fun `shows the place of the player in a chapter of the book`() {
    val shown = book.overlay(livePlaybackState(chapterId = ChapterId("chapter-2")))

    assertEquals(expected = ChapterId("chapter-2"), actual = shown.content.currentChapter)
    assertEquals(expected = 1234L, actual = shown.content.positionInChapter)
    assertEquals(expected = 1.5f, actual = shown.content.playbackSpeed)
  }

  @Test
  fun `keeps the saved place when the player is in a chapter the book no longer has`() {
    val shown = book.overlay(livePlaybackState(chapterId = ChapterId("deleted-chapter")))

    assertEquals(expected = book, actual = shown)
  }

  @Test
  fun `keeps the saved place when the player has another book`() {
    val shown =
      book.overlay(
        livePlaybackState(chapterId = ChapterId("chapter-2"), bookId = BookId("another-book")),
      )

    assertEquals(expected = book, actual = shown)
  }

  private fun livePlaybackState(
    chapterId: ChapterId,
    bookId: BookId = book.id,
  ) =
    LivePlaybackState(
      bookId = bookId,
      chapterId = chapterId,
      positionMs = 1234L,
      isPlaying = true,
      playbackSpeed = 1.5f,
    )

  private fun chapter(id: String) =
    Chapter(
      id = ChapterId(id),
      name = id,
      duration = 10_000,
      fileLastModified = Instant.fromEpochMilliseconds(0),
      markData = emptyList(),
      fileSize = 0,
    )
}

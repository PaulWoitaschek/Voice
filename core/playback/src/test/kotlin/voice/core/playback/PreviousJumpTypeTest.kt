package voice.core.playback

import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.playback.history.PlaybackPosition
import voice.core.playback.history.isUndoableJump
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreviousJumpTypeTest {

  private val chapter = ChapterId("chapter")
  private val previousChapter = ChapterId("previous")

  private fun at(
    time: Long,
    chapterId: ChapterId = chapter,
  ) = PlaybackPosition(BookId("book"), chapterId, time)

  private fun undoable(
    type: ListeningEvent.Type,
    from: PlaybackPosition,
    to: PlaybackPosition,
  ) = ListeningEvent(
    bookId = from.bookId,
    type = type,
    source = ListeningEvent.Source.App,
    atMillis = 0,
    chapterId = from.chapterId,
    time = from.time,
    toChapterId = to.chapterId,
    toTime = to.time,
  ).isUndoableJump()

  @Test
  fun `restarting the chapter a few seconds in is a seek that can't be undone`() {
    val type = assertNotNull(previousJumpType(from = at(5_000), to = at(0), sameItem = true))

    assertEquals(expected = ListeningEvent.Type.Seek, actual = type)
    assertFalse(undoable(type, from = at(5_000), to = at(0)))
  }

  @Test
  fun `restarting the chapter far in can be undone`() {
    val type = assertNotNull(previousJumpType(from = at(60_000), to = at(0), sameItem = true))

    assertEquals(expected = ListeningEvent.Type.Seek, actual = type)
    assertTrue(undoable(type, from = at(60_000), to = at(0)))
  }

  @Test
  fun `going to the previous chapter is a chapter change`() {
    assertEquals(
      expected = ListeningEvent.Type.ChapterChange,
      actual = previousJumpType(from = at(1_000), to = at(0, previousChapter), sameItem = false),
    )
  }

  @Test
  fun `nothing is recorded at the start of the first chapter`() {
    assertNull(previousJumpType(from = at(0), to = at(0), sameItem = true))
  }

  @Test
  fun `nothing is recorded while the chapters are not known yet`() {
    assertNull(previousJumpType(from = at(7_000), to = null, sameItem = true))
  }
}

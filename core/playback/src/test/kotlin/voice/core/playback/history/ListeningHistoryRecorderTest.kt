package voice.core.playback.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.ListeningHistoryRepo
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class ListeningHistoryRecorderTest {

  private val scope = TestScope()
  private val repo = RecordingRepo()
  private val recorder = ListeningHistoryRecorder(
    repo = repo,
    clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC),
    scope = scope,
  )
  private val chapter = ChapterId("chapter")
  private val otherChapter = ChapterId("other")

  private fun at(
    time: Long,
    chapterId: ChapterId = chapter,
  ) = PlaybackPosition(BookId("book"), chapterId, time)

  private fun seek(
    from: PlaybackPosition,
    to: PlaybackPosition,
    type: ListeningEvent.Type = ListeningEvent.Type.Seek,
  ) = recorder.record(type, ListeningEvent.Source.App, from, to)

  @Test
  fun `big seeks and chapter changes can be undone`() = scope.runTest {
    seek(at(0), at(29_000))
    assertNull(recorder.lastJump.value)

    seek(at(0), at(30_000))
    assertEquals(expected = at(0), actual = recorder.lastJump.value?.from)

    seek(at(5_000), at(0, otherChapter), type = ListeningEvent.Type.ChapterChange)
    assertEquals(expected = at(5_000), actual = recorder.lastJump.value?.from)
  }

  @Test
  fun `skipping does not replace the jump to undo`() = scope.runTest {
    seek(at(0), at(60_000))
    recorder.record(ListeningEvent.Type.SkipForward, ListeningEvent.Source.Headset, at(60_000), at(90_000), value = "30")

    assertEquals(expected = at(0), actual = recorder.lastJump.value?.from)
  }

  @Test
  fun `jumping back clears the jump`() = scope.runTest {
    seek(at(0), at(60_000))
    seek(at(60_000), at(0), type = ListeningEvent.Type.JumpBack)

    assertNull(recorder.lastJump.value)
    advanceUntilIdle()
    assertEquals(
      expected = listOf(ListeningEvent.Type.Seek, ListeningEvent.Type.JumpBack),
      actual = repo.events.map { it.type },
    )
  }

  @Test
  fun `dragging the speed slider records only where it stopped`() = scope.runTest {
    listOf("1.1", "1.2", "1.3").forEach { speed ->
      recorder.record(ListeningEvent.Type.SpeedChanged, ListeningEvent.Source.App, at(0), value = speed)
      advanceTimeBy(0.5.seconds)
    }
    assertEquals(expected = emptyList(), actual = repo.events)

    advanceUntilIdle()
    assertEquals(expected = listOf("1.3"), actual = repo.events.map { it.value })
  }

  @Test
  fun `speed changes of different books are each recorded`() = scope.runTest {
    val otherBook = PlaybackPosition(BookId("other"), chapter, 0)
    recorder.record(ListeningEvent.Type.SpeedChanged, ListeningEvent.Source.App, at(0), value = "1.2")
    recorder.record(ListeningEvent.Type.SpeedChanged, ListeningEvent.Source.App, otherBook, value = "1.5")

    advanceUntilIdle()
    assertEquals(
      expected = listOf(BookId("book") to "1.2", BookId("other") to "1.5"),
      actual = repo.events.map { it.bookId to it.value },
    )
  }
}

class RecordingRepo : ListeningHistoryRepo {

  val events = mutableListOf<ListeningEvent>()

  override fun events(bookId: BookId): Flow<List<ListeningEvent>> = emptyFlow()

  override suspend fun add(event: ListeningEvent) {
    events += event
  }

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
    chapters: Map<ChapterId, ChapterId>,
  ) = Unit
}

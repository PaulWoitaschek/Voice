package voice.core.playback.history

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.playback.stats.ListeningTallyRecorder

class ListeningHistoryRecorderTest {

  private val scope = TestScope()
  private val events = mutableListOf<ListeningEvent>()
  private val repo =
    mockk<ListeningHistoryRepo> {
      coEvery { add(capture(events)) } just Runs
    }
  private val tally = mockk<ListeningTallyRecorder>(relaxUnitFun = true)
  private val recorder =
    ListeningHistoryRecorder(
      repo = repo,
      clock =
        object : Clock {
          override fun now(): Instant = Instant.parse("2026-10-07T10:00:00Z")
        },
      scope = scope,
      tally = tally,
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
    recorder.record(
      ListeningEvent.Type.SkipForward,
      ListeningEvent.Source.Headset,
      at(60_000),
      at(90_000),
    )

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
      actual = events.map { it.type },
    )
  }

  private fun sleepTimer(type: ListeningEvent.Type) =
    recorder.record(type, ListeningEvent.Source.SleepTimer, at(0))

  @Test
  fun `the sleep timer runs out at 23_40, one night, the day and the book`() = scope.runTest {
    sleepTimer(ListeningEvent.Type.SleepTimerStarted)
    sleepTimer(ListeningEvent.Type.SleepTimerEnded)

    verify(exactly = 1) { tally.add(BookId("book"), "sleep_timer_night") }
    verify(exactly = 0) { tally.add(any(), "sleep_timer_shake") }
  }

  @Test
  fun `the timer stopped, shake, and the timer runs out again, the same night, with 1 shake`() =
    scope.runTest {
      sleepTimer(ListeningEvent.Type.SleepTimerStarted)
      sleepTimer(ListeningEvent.Type.SleepTimerEnded)
      sleepTimer(ListeningEvent.Type.SleepTimerExtended)
      sleepTimer(ListeningEvent.Type.SleepTimerEnded)

      verify(exactly = 1) { tally.add(BookId("book"), "sleep_timer_night") }
      verify(exactly = 1) { tally.add(BookId("book"), "sleep_timer_shake") }
    }

  @Test
  fun `a shake while the sound fades out counts too`() = scope.runTest {
    sleepTimer(ListeningEvent.Type.SleepTimerStarted)
    sleepTimer(ListeningEvent.Type.SleepTimerExtended)
    sleepTimer(ListeningEvent.Type.SleepTimerEnded)

    verify(exactly = 1) { tally.add(BookId("book"), "sleep_timer_night") }
    verify(exactly = 1) { tally.add(BookId("book"), "sleep_timer_shake") }
  }

  @Test
  fun `a sleep timer that is started again by hand counts a new night`() = scope.runTest {
    sleepTimer(ListeningEvent.Type.SleepTimerStarted)
    sleepTimer(ListeningEvent.Type.SleepTimerEnded)
    sleepTimer(ListeningEvent.Type.SleepTimerStarted)
    sleepTimer(ListeningEvent.Type.SleepTimerEnded)

    verify(exactly = 2) { tally.add(BookId("book"), "sleep_timer_night") }
  }
}

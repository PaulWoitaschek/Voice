package voice.features.bookmark.history

import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.features.bookmark.BookIndex
import voice.features.bookmark.DayLabel
import voice.features.bookmark.chapterTime
import voice.features.bookmark.testBook
import voice.features.bookmark.testChapter
import voice.features.bookmark.testEvent
import voice.features.bookmark.testZone
import voice.features.bookmark.today
import voice.features.bookmark.yesterday
import java.time.Instant
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HistoryStretchesTest {

  private val arrival = testChapter()
  private val signal = testChapter()
  private val book = testBook(listOf(arrival, signal))

  private fun stretches(
    vararg events: ListeningEvent,
    playing: Boolean = false,
    now: Instant = today(12),
    current: HistorySpot? = null,
  ): HistoryViewState = historyStretches(events.toList(), BookIndex(book), playing, now, testZone)
    .viewState(now, current)

  private fun HistoryViewState.single(): HistoryStretch = all().single()

  private fun HistoryViewState.all(): List<HistoryStretch> = rows().filterIsInstance<HistoryStretch>()

  private fun HistoryViewState.jumps(): List<HistoryJump> = rows().filterIsInstance<HistoryJump>()

  private fun HistoryViewState.rows(): List<HistoryRow> = days.flatMap { it.rows }

  private fun inArrival(time: Duration) = HistoryLocation(1, chapterTime(time))

  private fun inSignal(time: Duration) = HistoryLocation(2, chapterTime(time))

  @Test
  fun `a play and a pause are one stretch`() {
    val stretch = stretches(
      testEvent(Type.Play, today(9), arrival, 2.minutes, source = Source.Headset),
      testEvent(Type.Pause, today(9, 20), signal, 3.minutes),
    ).single()

    assertEquals(expected = LocalTime.of(9, 0), actual = stretch.start)
    assertEquals(expected = LocalTime.of(9, 20), actual = stretch.end)
    assertEquals(expected = 20.minutes, actual = stretch.listened)
    assertEquals(expected = inArrival(2.minutes), actual = stretch.from)
    assertEquals(expected = inSignal(3.minutes), actual = stretch.to)
    assertEquals(expected = StretchCause.Played(Source.Headset), actual = stretch.cause)
    assertEquals(expected = HistorySpot(signal.id, 3.minutes.inWholeMilliseconds), actual = stretch.goTo?.spot)
  }

  @Test
  fun `a short pause goes on in the same stretch`() {
    val stretch = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 5.minutes),
      testEvent(Type.Play, today(9, 20), arrival, 5.minutes),
      testEvent(Type.Pause, today(9, 30), arrival, 9.minutes),
    ).single()

    assertEquals(expected = LocalTime.of(9, 30), actual = stretch.end)
    assertEquals(expected = 20.minutes, actual = stretch.listened)
    assertEquals(expected = inArrival(9.minutes), actual = stretch.to)
  }

  @Test
  fun `a pause of 15 minutes or more starts a new stretch`() {
    val stretches = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 5.minutes),
      testEvent(Type.Play, today(9, 25), arrival, 5.minutes),
      testEvent(Type.Pause, today(9, 30), arrival, 9.minutes),
    ).all()

    assertEquals(expected = listOf(LocalTime.of(9, 25), LocalTime.of(9, 0)), actual = stretches.map { it.start })
  }

  @Test
  fun `a big jump while playing ends the stretch where the jump left`() {
    val stretches = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Seek, today(9, 10), arrival, 5.minutes, source = Source.Notification, to = signal to 1.minutes),
      testEvent(Type.Pause, today(9, 20), signal, 6.minutes),
    ).all()

    val (afterJump, beforeJump) = stretches
    assertEquals(expected = inArrival(5.minutes), actual = beforeJump.to)
    assertEquals(expected = HistorySpot(arrival.id, 5.minutes.inWholeMilliseconds), actual = beforeJump.goTo?.spot)
    assertEquals(expected = inSignal(1.minutes), actual = afterJump.from)
    assertEquals(expected = StretchCause.Jumped(Type.Seek, Source.Notification, hasRow = true), actual = afterJump.cause)
  }

  @Test
  fun `a big jump while paused starts the next stretch`() {
    val stretches = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 5.minutes),
      testEvent(Type.ChapterChange, today(9, 11), arrival, 5.minutes, to = signal to 0.minutes),
      testEvent(Type.Play, today(9, 12), signal, 0.minutes),
      testEvent(Type.Pause, today(9, 30), signal, 8.minutes),
    ).all()

    assertEquals(
      expected = listOf(StretchCause.Jumped(Type.ChapterChange, Source.App, hasRow = true), StretchCause.Played(Source.App)),
      actual = stretches.map { it.cause },
    )
  }

  @Test
  fun `skips and small seeks stay in the stretch`() {
    val stretch = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.SkipForward, today(9, 5), arrival, 5.minutes),
      testEvent(Type.Seek, today(9, 6), arrival, 6.minutes, to = arrival to 6.minutes + 20.seconds),
      testEvent(Type.Pause, today(9, 10), arrival, 9.minutes),
    ).single()

    assertEquals(expected = inArrival(0.minutes), actual = stretch.from)
    assertEquals(expected = inArrival(9.minutes), actual = stretch.to)
  }

  @Test
  fun `the sleep timer ending marks the stretch, which goes to where the listener was still awake`() {
    val stretch = stretches(
      testEvent(Type.Play, yesterday(22, 40), arrival, 2.minutes),
      testEvent(Type.SleepTimerEnded, yesterday(23, 10), signal, 4.minutes, source = Source.SleepTimer),
    ).single()

    assertEquals(expected = StretchCause.SleepTimerEnded, actual = stretch.cause)
    assertEquals(expected = HistorySpot(arrival.id, 2.minutes.inWholeMilliseconds), actual = stretch.goTo?.spot)
    assertEquals(expected = inArrival(2.minutes), actual = stretch.goTo?.location)
  }

  @Test
  fun `a night goes to where the listener last played`() {
    val stretch = stretches(
      testEvent(Type.Play, yesterday(21), arrival, 0.minutes),
      testEvent(Type.Pause, yesterday(22), arrival, 9.minutes),
      testEvent(Type.Play, yesterday(22, 5), signal, 2.minutes),
      testEvent(Type.SleepTimerEnded, yesterday(22, 35), signal, 8.minutes, source = Source.SleepTimer),
    ).single()

    assertEquals(expected = HistorySpot(signal.id, 2.minutes.inWholeMilliseconds), actual = stretch.goTo?.spot)
  }

  @Test
  fun `the end of chapter timer stopping at the end of the book still marks the night`() {
    val stretch = stretches(
      testEvent(Type.Play, yesterday(22, 40), arrival, 2.minutes),
      testEvent(Type.Pause, yesterday(23, 10), signal, 9.minutes, source = Source.Unknown),
      testEvent(Type.SleepTimerEnded, yesterday(23, 10), signal, 9.minutes, source = Source.SleepTimer),
    ).single()

    assertEquals(expected = StretchCause.SleepTimerEnded, actual = stretch.cause)
  }

  @Test
  fun `a stretch that lost its pause with nothing after the play keeps its start`() {
    val stretch = stretches(
      testEvent(Type.Play, today(8), arrival, 3.minutes),
    ).single()

    assertEquals(expected = inArrival(3.minutes), actual = stretch.from)
    assertNull(stretch.to)
    assertNull(stretch.end)
    assertNull(stretch.listened)
    assertEquals(expected = HistorySpot(arrival.id, 3.minutes.inWholeMilliseconds), actual = stretch.goTo?.spot)
  }

  @Test
  fun `a night that only a sleep timer bookmark remembered has a start`() {
    val stretch = stretches(
      testEvent(Type.SleepTimerSet, yesterday(22, 40), arrival, 2.minutes, source = Source.SleepTimer),
    ).single()

    assertEquals(expected = StretchCause.SleepTimerStarted, actual = stretch.cause)
    assertEquals(expected = inArrival(2.minutes), actual = stretch.from)
    assertNull(stretch.to)
    assertNull(stretch.end)
    assertEquals(expected = HistorySpot(arrival.id, 2.minutes.inWholeMilliseconds), actual = stretch.goTo?.spot)
  }

  @Test
  fun `a sleep timer set while playing is part of the stretch`() {
    val stretch = stretches(
      testEvent(Type.Play, yesterday(22, 40), arrival, 2.minutes),
      testEvent(Type.SleepTimerSet, yesterday(22, 40), arrival, 2.minutes, source = Source.SleepTimer),
      testEvent(Type.Pause, yesterday(23), arrival, 8.minutes),
    ).single()

    assertEquals(expected = StretchCause.Played(Source.App), actual = stretch.cause)
  }

  @Test
  fun `a lost pause ends the stretch with its last event`() {
    val stretches = stretches(
      testEvent(Type.Play, today(8), arrival, 0.minutes),
      testEvent(Type.SkipForward, today(8, 5), arrival, 5.minutes),
      testEvent(Type.Play, today(9), signal, 0.minutes),
      testEvent(Type.Pause, today(9, 10), signal, 9.minutes),
    ).all()

    val (_, lost) = stretches
    assertEquals(expected = LocalTime.of(8, 5), actual = lost.end)
    assertEquals(expected = 5.minutes, actual = lost.listened)
  }

  @Test
  fun `moments between skipping chapters are left out`() {
    val stretches = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.ChapterChange, today(9, 10), arrival, 8.minutes, to = signal to 0.minutes),
      testEvent(
        Type.ChapterChange,
        Instant.ofEpochMilli(today(9, 10).toEpochMilli() + 3_000),
        signal,
        3.seconds,
        to = arrival to 0.minutes,
      ),
      testEvent(Type.Pause, today(9, 20), arrival, 9.minutes),
    ).all()

    assertEquals(expected = listOf(LocalTime.of(9, 10), LocalTime.of(9, 0)), actual = stretches.map { it.start })
  }

  @Test
  fun `the playing stretch follows the time and the position`() {
    val state = stretches(
      testEvent(Type.Play, today(11, 30), arrival, 0.minutes),
      playing = true,
      now = today(12),
      current = HistorySpot(signal.id, 4.minutes.inWholeMilliseconds),
    )

    val stretch = state.single()
    assertEquals(expected = LocalTime.of(12, 0), actual = stretch.end)
    assertEquals(expected = 30.minutes, actual = stretch.listened)
    assertEquals(expected = inSignal(4.minutes), actual = stretch.to)
    assertNull(stretch.goTo)
  }

  @Test
  fun `stretches are newest first, under the part of the day they started in`() {
    val state = stretches(
      testEvent(Type.Play, yesterday(15), arrival, 0.minutes),
      testEvent(Type.Pause, yesterday(15, 30), arrival, 9.minutes),
      testEvent(Type.Play, today(8), signal, 0.minutes),
      testEvent(Type.Pause, today(8, 20), signal, 9.minutes),
    )

    assertEquals(expected = listOf(DayLabel.ThisMorning, DayLabel.Yesterday), actual = state.days.map { it.label })
  }

  @Test
  fun `a spot that's gone after a rescan can't be gone to`() {
    val gone = testChapter()
    val stretch = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Pause, today(9, 20), gone, 3.minutes),
    ).single()

    assertNull(stretch.to)
    assertNull(stretch.goTo)
  }

  @Test
  fun `a jump with nothing heard before it still shows where it left`() {
    val jump = stretches(
      testEvent(Type.Seek, today(9), arrival, 0.minutes, to = arrival to 5.minutes),
    ).rows().single()

    assertEquals(
      expected = HistoryJump(
        key = jump.key,
        at = LocalTime.of(9, 0),
        from = inArrival(0.minutes),
        to = inArrival(5.minutes),
        type = Type.Seek,
        source = Source.App,
        back = HistoryTarget(HistorySpot(arrival.id, 0L), inArrival(0.minutes)),
      ),
      actual = jump,
    )
  }

  @Test
  fun `a jump while playing sits between the stretch it ended and the one it started`() {
    val rows = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.ChapterChange, today(9, 10), arrival, 5.minutes, source = Source.Headset, to = signal to 0.minutes),
      testEvent(Type.Pause, today(9, 20), signal, 6.minutes),
    ).rows()

    val (after, jump, before) = rows
    assertEquals(expected = inSignal(0.minutes), actual = (after as HistoryStretch).from)
    assertEquals(expected = inArrival(5.minutes), actual = (before as HistoryStretch).to)
    jump as HistoryJump
    assertEquals(expected = Type.ChapterChange to Source.Headset, actual = jump.type to jump.source)
    assertEquals(expected = HistorySpot(arrival.id, 5.minutes.inWholeMilliseconds), actual = jump.back.spot)
  }

  @Test
  fun `a jump right after pressing play still shows where it left`() {
    val state = stretches(
      testEvent(Type.Play, today(9), arrival, 2.minutes),
      testEvent(Type.Seek, today(9).plusSeconds(4), arrival, 2.minutes + 4.seconds, to = signal to 3.minutes),
      testEvent(Type.Pause, today(9).plusSeconds(7), signal, 3.minutes + 3.seconds),
    )

    assertEquals(expected = emptyList(), actual = state.all())
    assertEquals(expected = HistorySpot(arrival.id, (2.minutes + 4.seconds).inWholeMilliseconds), actual = state.jumps().single().back.spot)
  }

  @Test
  fun `a jump while paused shows right away`() {
    val rows = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 5.minutes),
      testEvent(Type.BookmarkJump, today(9, 11), arrival, 5.minutes, to = signal to 2.minutes),
    ).rows()

    assertEquals(expected = listOf(HistoryJump::class, HistoryStretch::class), actual = rows.map { it::class })
  }

  @Test
  fun `jumps without listening in between are one row, from where the first one left`() {
    val jump = stretches(
      testEvent(Type.ChapterChange, today(9), arrival, 5.minutes, source = Source.Car, to = signal to 0.minutes),
      testEvent(Type.Seek, today(9, 1), signal, 0.minutes, to = signal to 4.minutes),
      testEvent(Type.Seek, today(9, 2), signal, 4.minutes, to = signal to 7.minutes),
    ).jumps().single()

    assertEquals(expected = inArrival(5.minutes), actual = jump.from)
    assertEquals(expected = inSignal(7.minutes), actual = jump.to)
    assertEquals(expected = LocalTime.of(9, 0), actual = jump.at)
    assertEquals(expected = Type.ChapterChange to Source.Car, actual = jump.type to jump.source)
  }

  @Test
  fun `a jump undone right away leaves no row`() {
    val state = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Seek, today(9, 10), arrival, 5.minutes, to = signal to 1.minutes),
      testEvent(Type.JumpBack, today(9, 10).plusSeconds(3), signal, 1.minutes + 3.seconds, to = arrival to 5.minutes),
      testEvent(Type.Pause, today(9, 20), arrival, 15.minutes),
    )

    assertEquals(expected = emptyList(), actual = state.jumps())
  }

  @Test
  fun `a stretch after a jump undone right away still says what started it`() {
    val newest = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Seek, today(9, 10), arrival, 5.minutes, to = signal to 1.minutes),
      testEvent(Type.JumpBack, today(9, 10).plusSeconds(3), signal, 1.minutes + 3.seconds, to = arrival to 5.minutes),
      testEvent(Type.Pause, today(9, 20), arrival, 15.minutes),
    ).all().first()

    assertEquals(expected = StretchCause.Jumped(Type.JumpBack, Source.App, hasRow = false), actual = newest.cause)
  }

  @Test
  fun `a stretch after several jumps shows the first one, like the jump's row`() {
    val stretch = stretches(
      testEvent(Type.ChapterChange, today(9), arrival, 5.minutes, source = Source.Car, to = signal to 0.minutes),
      testEvent(Type.Seek, today(9).plusSeconds(20), signal, 0.minutes, to = signal to 4.minutes),
      testEvent(Type.Play, today(9, 1), signal, 4.minutes),
      testEvent(Type.Pause, today(9, 10), signal, 9.minutes),
    ).single()

    assertEquals(expected = StretchCause.Jumped(Type.ChapterChange, Source.Car, hasRow = true), actual = stretch.cause)
  }

  @Test
  fun `going back after listening for a while is a jump of its own`() {
    val jumps = stretches(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Seek, today(9, 10), arrival, 5.minutes, to = signal to 1.minutes),
      testEvent(Type.JumpBack, today(9, 15), signal, 6.minutes, to = arrival to 5.minutes),
      testEvent(Type.Pause, today(9, 20), arrival, 9.minutes),
    ).jumps()

    assertEquals(
      expected = listOf(inSignal(6.minutes) to inArrival(5.minutes), inArrival(5.minutes) to inSignal(1.minutes)),
      actual = jumps.map { it.from to it.to },
    )
  }

  @Test
  fun `jumps a while apart are separate rows`() {
    val jumps = stretches(
      testEvent(Type.Seek, today(9), arrival, 0.minutes, to = arrival to 5.minutes),
      testEvent(Type.Seek, today(9, 20), arrival, 5.minutes, to = signal to 1.minutes),
    ).jumps()

    assertEquals(expected = listOf(LocalTime.of(9, 20), LocalTime.of(9, 0)), actual = jumps.map { it.at })
  }

  @Test
  fun `a jump to a spot that's gone after a rescan still goes back`() {
    val gone = testChapter()
    val jump = stretches(
      testEvent(Type.Seek, today(9), arrival, 3.minutes, to = gone to 1.minutes),
    ).jumps().single()

    assertNull(jump.to)
    assertEquals(expected = HistorySpot(arrival.id, 3.minutes.inWholeMilliseconds), actual = jump.back.spot)
  }
}

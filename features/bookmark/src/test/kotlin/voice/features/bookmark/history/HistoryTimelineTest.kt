package voice.features.bookmark.history

import voice.core.data.Book
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
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HistoryTimelineTest {

  private val arrival = testChapter()
  private val signal = testChapter()
  private val book = testBook(listOf(arrival, signal))

  private fun timeline(
    vararg events: ListeningEvent,
    book: Book = this.book,
  ): HistoryViewState {
    // newest first, like the repo
    return historyViewState(events.reversed(), BookIndex(book), now = today(20), zone = testZone)
  }

  private fun HistoryViewState.rows(): List<HistoryRow> = days.flatMap { day -> day.sessions.flatMap { it.rows } }

  private fun HistoryViewState.events(): List<HistoryEvent> = rows().filterIsInstance<HistoryEvent>()

  private fun inArrival(time: Duration) =
    HistoryPlace(HistorySpot(arrival.id, time.inWholeMilliseconds), HistoryLocation(1, chapterTime(time)))

  private fun inSignal(time: Duration) =
    HistoryPlace(HistorySpot(signal.id, time.inWholeMilliseconds), HistoryLocation(2, chapterTime(time)))

  @Test
  fun `the newest comes first`() {
    val state = timeline(
      testEvent(Type.Play, yesterday(9), arrival, 1.minutes),
      testEvent(Type.Pause, yesterday(9, 10), arrival, 2.minutes),
      testEvent(Type.Play, today(9), arrival, 3.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 4.minutes),
    )

    assertEquals(expected = listOf(DayLabel.ThisMorning, DayLabel.Yesterday), actual = state.days.map { it.label })
    assertEquals(
      expected = listOf(Type.Pause to LocalTime.of(9, 10), Type.Play to LocalTime.of(9, 0)),
      actual = state.days.first().sessions.single().rows.map { (it as HistoryEvent).type to it.at },
    )
  }

  @Test
  fun `every event shows where it happened`() {
    val play = testEvent(Type.Play, today(9), arrival, 1.minutes, source = Source.Headset)
    val pause = testEvent(Type.Pause, today(9, 10), signal, 2.minutes, source = Source.Car)

    assertEquals(
      expected = listOf(
        HistoryEvent(pause.id, LocalTime.of(9, 10), playingBefore = true, Type.Pause, Source.Car, inSignal(2.minutes)),
        HistoryEvent(play.id, LocalTime.of(9, 0), playingBefore = false, Type.Play, Source.Headset, inArrival(1.minutes)),
      ),
      actual = timeline(play, pause).events(),
    )
  }

  @Test
  fun `the line is solid while playing and dotted while paused`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.ChapterChange, today(9, 5), arrival, 9.minutes, to = signal to Duration.ZERO),
      testEvent(Type.Pause, today(9, 10), signal, 2.minutes),
      testEvent(Type.BookmarkJump, today(9, 20), signal, 2.minutes, to = arrival to 4.minutes),
      testEvent(Type.Play, today(9, 30), arrival, 4.minutes),
    )

    assertEquals(expected = listOf(false, false, true, true, false), actual = state.rows().map { it.playingBefore })
  }

  @Test
  fun `a long pause starts a new card and a short one doesn't`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 2.minutes),
      testEvent(Type.Play, today(9, 39), arrival, 2.minutes),
      testEvent(Type.Pause, today(9, 50), arrival, 3.minutes),
      testEvent(Type.Play, today(10, 20), arrival, 3.minutes),
    )

    val sessions = state.days.single().sessions
    assertEquals(expected = listOf(1, 4), actual = sessions.map { it.rows.size })
    assertEquals(expected = LocalTime.of(10, 20), actual = sessions.first().rows.single().at)
  }

  @Test
  fun `listening without events stays one card`() {
    val long = testChapter(2.hours)
    val state = timeline(
      testEvent(Type.Play, yesterday(22), long, 1.minutes),
      testEvent(Type.SleepTimerEnded, yesterday(23, 30), long, 1.hours + 31.minutes, source = Source.SleepTimer),
      book = testBook(listOf(long)),
    )

    assertEquals(expected = 1, actual = state.days.single().sessions.size)
  }

  @Test
  fun `a lost pause doesn't keep the card going`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.BookmarkJump, today(10), arrival, 3.minutes, to = signal to 1.minutes),
      testEvent(Type.Play, today(10, 1), signal, 1.minutes),
    )

    assertEquals(expected = listOf(2, 1), actual = state.days.single().sessions.map { it.rows.size })
    assertEquals(expected = listOf(false, false, false), actual = state.rows().map { it.playingBefore })
  }

  @Test
  fun `a play after a lost pause starts a new card`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Play, today(10), arrival, 5.minutes),
    )

    assertEquals(expected = 2, actual = state.days.single().sessions.size)
    assertEquals(expected = listOf(false, false), actual = state.rows().map { it.playingBefore })
  }

  @Test
  fun `a card stays under the part of the day it started in`() {
    val state = timeline(
      testEvent(Type.Play, today(11, 50), arrival, 1.minutes),
      testEvent(Type.Pause, today(12, 10), arrival, 5.minutes),
    )

    assertEquals(expected = listOf(DayLabel.ThisMorning), actual = state.days.map { it.label })
    assertEquals(expected = listOf(true, false), actual = state.rows().map { it.playingBefore })
  }

  @Test
  fun `a long call stays in the card`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Play, today(9, 50), arrival, 2.minutes, source = Source.AudioFocus),
    )

    val rows = state.days.single().sessions.single().rows
    assertEquals(expected = 40.minutes, actual = (rows.first() as HistoryInterruption).duration)
  }

  @Test
  fun `taps in a row in the same direction add up`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.SkipBack, today(9, 1), arrival, 2.minutes),
      testEvent(Type.SkipBack, today(9, 1).plusSeconds(20), arrival, 2.minutes),
      testEvent(Type.SkipBack, today(9, 1).plusSeconds(40), arrival, 2.minutes),
      testEvent(Type.SkipForward, today(9, 2), arrival, 2.minutes),
      testEvent(Type.SkipForward, today(9, 2).plusSeconds(30), arrival, 2.minutes),
      testEvent(Type.SkipForward, today(9, 20), arrival, 5.minutes),
    )

    val skips = state.rows().filterIsInstance<HistorySkips>()
    assertEquals(expected = listOf(false to 1, false to 2, true to 3), actual = skips.map { it.back to it.count })
    assertEquals(expected = listOf(LocalTime.of(9, 20), LocalTime.of(9, 2), LocalTime.of(9, 1)), actual = skips.map { it.at })
    assertEquals(expected = true, actual = skips.all { it.playingBefore })
  }

  @Test
  fun `a small seek is left out`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 2.minutes),
      testEvent(Type.Seek, today(9, 20), arrival, 2.minutes - 2.seconds, to = arrival to 2.minutes),
    )

    assertEquals(expected = listOf(Type.Pause, Type.Play), actual = state.rows().map { (it as? HistoryEvent)?.type })
  }

  @Test
  fun `a big seek is a jump with both ends`() {
    val state = timeline(
      testEvent(Type.Seek, today(9), arrival, 1.minutes, source = Source.Notification, to = signal to 3.minutes),
    )

    val jump = state.events().single()
    assertEquals(expected = Type.Seek, actual = jump.type)
    assertEquals(expected = inArrival(1.minutes), actual = jump.place)
    assertEquals(expected = inSignal(3.minutes), actual = jump.to)
  }

  @Test
  fun `a jump back is a jump of its own`() {
    val state = timeline(
      testEvent(Type.ChapterChange, today(9), arrival, 4.minutes, to = signal to Duration.ZERO),
      testEvent(Type.JumpBack, today(9, 1), signal, 20.seconds, to = arrival to 4.minutes),
    )

    assertEquals(expected = listOf(Type.JumpBack, Type.ChapterChange), actual = state.events().map { it.type })
  }

  @Test
  fun `a call is one row with how long it lasted`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Play, today(9, 13), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Pause, today(9, 20), arrival, 3.minutes),
    )

    val rows = state.rows()
    assertEquals(expected = 3, actual = rows.size)
    val interruption = rows[1] as HistoryInterruption
    assertEquals(expected = 3.minutes, actual = interruption.duration)
    assertEquals(expected = LocalTime.of(9, 10), actual = interruption.at)
    assertEquals(expected = listOf(true, true, false), actual = rows.map { it.playingBefore })
  }

  @Test
  fun `a navigation prompt is left out`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Play, today(9, 10), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Pause, today(9, 20), arrival, 3.minutes),
    )

    assertEquals(expected = listOf(Type.Pause, Type.Play), actual = state.events().map { it.type })
    assertEquals(expected = true, actual = state.events().first().playingBefore)
  }

  @Test
  fun `an interruption that doesn't end by itself is a pause`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Play, today(9, 30), arrival, 2.minutes, source = Source.Headset),
    )

    assertEquals(
      expected = listOf(Type.Play to Source.Headset, Type.Pause to Source.AudioFocus, Type.Play to Source.App),
      actual = state.events().map { it.type to it.source },
    )
  }

  @Test
  fun `a night with the sleep timer`() {
    val state = timeline(
      testEvent(Type.SleepTimerSet, yesterday(9), arrival, 1.minutes),
      testEvent(Type.Play, today(1), arrival, 1.minutes),
      testEvent(Type.SleepTimerStarted, today(1), arrival, 1.minutes),
      testEvent(Type.SleepTimerEnded, today(1, 20), arrival, 5.minutes, source = Source.SleepTimer),
      testEvent(Type.SleepTimerExtended, today(1, 20), arrival, 5.minutes, source = Source.SleepTimer),
      testEvent(Type.SleepTimerEnded, today(1, 40), signal, 3.minutes, source = Source.SleepTimer),
    )

    val night = state.days.first()
    assertEquals(expected = DayLabel.LastNight, actual = night.label)
    assertEquals(
      expected = listOf(
        Type.SleepTimerEnded to true,
        Type.SleepTimerExtended to false,
        Type.SleepTimerEnded to true,
        Type.SleepTimerStarted to true,
        Type.Play to false,
      ),
      actual = night.sessions.single().rows.map { (it as HistoryEvent).type to it.playingBefore },
    )
    assertEquals(expected = Type.SleepTimerSet, actual = (state.days.last().sessions.single().rows.single() as HistoryEvent).type)
  }

  @Test
  fun `events in a chapter that's gone are left out`() {
    val gone = testChapter()
    val state = timeline(
      testEvent(Type.Play, yesterday(9), gone, 1.minutes),
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.ChapterChange, today(9, 5), arrival, 9.minutes, to = gone to Duration.ZERO),
    )

    assertEquals(expected = listOf(DayLabel.ThisMorning), actual = state.days.map { it.label })
    assertEquals(expected = listOf(null, null), actual = state.events().map { it.to })
    assertEquals(expected = listOf(Type.ChapterChange, Type.Play), actual = state.events().map { it.type })
  }

  @Test
  fun `a book with one chapter shows no chapter number`() {
    val state = timeline(testEvent(Type.Play, today(9), arrival, 1.minutes), book = testBook(listOf(arrival)))

    assertEquals(expected = HistoryLocation(null, chapterTime(1.minutes)), actual = state.events().single().place.location)
  }
}

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
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HistoryTimelineTest {

  private val arrival = testChapter()
  private val signal = testChapter()
  private val book = testBook(listOf(arrival, signal))

  private fun timeline(vararg events: ListeningEvent): HistoryViewState {
    // newest first, like the repo
    return historyViewState(events.reversed(), BookIndex(book), now = today(20), zone = testZone)
  }

  private fun HistoryViewState.rows(): List<HistoryRow> = days.flatMap { it.rows }

  private fun HistoryViewState.events(): List<HistoryEvent> = rows().filterIsInstance<HistoryEvent>()

  private fun inArrival(time: Duration) =
    HistoryPlace(HistorySpot(arrival.id, time.inWholeMilliseconds), HistoryLocation(1, chapterTime(time)))

  private fun inSignal(time: Duration) =
    HistoryPlace(HistorySpot(signal.id, time.inWholeMilliseconds), HistoryLocation(2, chapterTime(time)))

  @Test
  fun `the newest part of the day comes first, and inside it the oldest row`() {
    val state = timeline(
      testEvent(Type.Play, yesterday(9), arrival, 1.minutes),
      testEvent(Type.Pause, yesterday(9, 10), arrival, 2.minutes),
      testEvent(Type.Play, today(9), arrival, 3.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 4.minutes),
    )

    assertEquals(expected = listOf(DayLabel.ThisMorning, DayLabel.Yesterday), actual = state.days.map { it.label })
    assertEquals(
      expected = listOf(Type.Play to LocalTime.of(9, 0), Type.Pause to LocalTime.of(9, 10)),
      actual = state.days.first().rows.map { (it as HistoryEvent).type to it.at },
    )
  }

  @Test
  fun `every event shows where it happened`() {
    val play = testEvent(Type.Play, today(9), arrival, 1.minutes, source = Source.Headset)
    val pause = testEvent(Type.Pause, today(9, 10), signal, 2.minutes, source = Source.Car)

    assertEquals(
      expected = listOf(
        HistoryEvent(play.id, LocalTime.of(9, 0), playingBefore = false, Type.Play, Source.Headset, inArrival(1.minutes)),
        HistoryEvent(pause.id, LocalTime.of(9, 10), playingBefore = true, Type.Pause, Source.Car, inSignal(2.minutes)),
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

    assertEquals(expected = listOf(false, true, true, false, false), actual = state.rows().map { it.playingBefore })
  }

  @Test
  fun `a play after a lost pause starts a new line`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Play, today(10), arrival, 5.minutes),
    )

    assertEquals(expected = listOf(false, false), actual = state.rows().map { it.playingBefore })
  }

  @Test
  fun `playing carries over into the next part of the day`() {
    val state = timeline(
      testEvent(Type.Play, today(11, 50), arrival, 1.minutes),
      testEvent(Type.Pause, today(12, 10), arrival, 5.minutes),
    )

    assertEquals(expected = listOf(DayLabel.ThisAfternoon, DayLabel.ThisMorning), actual = state.days.map { it.label })
    assertEquals(expected = true, actual = state.days.first().rows.single().playingBefore)
  }

  @Test
  fun `taps in the same direction add up`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.SkipBack, today(9, 1), arrival, 2.minutes),
      testEvent(Type.SkipBack, today(9, 2), arrival, 2.minutes),
      testEvent(Type.Seek, today(9, 3), arrival, 2.minutes, to = arrival to 1.minutes + 50.seconds),
      testEvent(Type.SkipForward, today(9, 4), arrival, 2.minutes),
      testEvent(Type.Seek, today(9, 5), arrival, 2.minutes, to = arrival to 2.minutes + 10.seconds),
      testEvent(Type.SkipBack, today(9, 6), arrival, 2.minutes),
    )

    val skips = state.rows().filterIsInstance<HistorySkips>()
    assertEquals(expected = listOf(true to 3, false to 2, true to 1), actual = skips.map { it.back to it.count })
    assertEquals(expected = listOf(LocalTime.of(9, 1), LocalTime.of(9, 4), LocalTime.of(9, 6)), actual = skips.map { it.at })
    assertEquals(expected = true, actual = skips.all { it.playingBefore })
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

    assertEquals(expected = listOf(Type.ChapterChange, Type.JumpBack), actual = state.events().map { it.type })
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
    assertEquals(expected = listOf(false, true, true), actual = rows.map { it.playingBefore })
  }

  @Test
  fun `a navigation prompt is left out`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Play, today(9, 10), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Pause, today(9, 20), arrival, 3.minutes),
    )

    assertEquals(expected = listOf(Type.Play, Type.Pause), actual = state.events().map { it.type })
    assertEquals(expected = true, actual = state.events().last().playingBefore)
  }

  @Test
  fun `an interruption that doesn't end by itself is a pause`() {
    val state = timeline(
      testEvent(Type.Play, today(9), arrival, 1.minutes),
      testEvent(Type.Pause, today(9, 10), arrival, 2.minutes, source = Source.AudioFocus),
      testEvent(Type.Play, today(9, 30), arrival, 2.minutes, source = Source.Headset),
    )

    assertEquals(
      expected = listOf(Type.Play to Source.App, Type.Pause to Source.AudioFocus, Type.Play to Source.Headset),
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
        Type.Play to false,
        Type.SleepTimerStarted to true,
        Type.SleepTimerEnded to true,
        Type.SleepTimerExtended to false,
        Type.SleepTimerEnded to true,
      ),
      actual = night.rows.map { (it as HistoryEvent).type to it.playingBefore },
    )
    assertEquals(expected = Type.SleepTimerSet, actual = state.days.last().rows.map { (it as HistoryEvent).type }.single())
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
    assertEquals(expected = listOf(Type.Play, Type.ChapterChange), actual = state.events().map { it.type })
  }

  @Test
  fun `a book with one chapter shows no chapter number`() {
    val state = historyViewState(
      listOf(testEvent(Type.Play, today(9), arrival, 1.minutes)),
      BookIndex(testBook(listOf(arrival))),
      now = today(20),
      zone = testZone,
    )

    assertEquals(expected = HistoryLocation(null, chapterTime(1.minutes)), actual = state.events().single().place.location)
  }
}

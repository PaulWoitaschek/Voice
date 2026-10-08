package voice.features.bookmark.history

import voice.core.data.Bookmark
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.data.at
import voice.features.bookmark.BookIndex
import voice.features.bookmark.DayLabel
import voice.features.bookmark.chapterTime
import voice.features.bookmark.testBook
import voice.features.bookmark.testBookmark
import voice.features.bookmark.testChapter
import voice.features.bookmark.testEvent
import voice.features.bookmark.testZone
import voice.features.bookmark.today
import voice.features.bookmark.yesterday
import java.time.Instant
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class HistoryTimelineTest {

  private val arrival = testChapter()
  private val signal = testChapter()
  private val static = testChapter()
  private val book = testBook(listOf(arrival, signal, static), currentChapter = signal.id, positionInChapter = 5.minutes)
  private val index = BookIndex(book)

  private fun timeline(
    vararg events: ListeningEvent,
    playing: Boolean = false,
    bookmarks: List<Bookmark> = emptyList(),
    filter: HistoryFilter? = null,
    source: Source? = null,
    now: Instant = today(10),
  ): HistoryTimeline = historyTimeline(
    events = events.toList(),
    index = index,
    bookmarks = bookmarks,
    playing = playing,
    selectedFilter = filter,
    selectedSource = source,
    now = now,
    zone = testZone,
  )

  private fun state(
    vararg events: ListeningEvent,
    playing: Boolean = false,
    bookmarks: List<Bookmark> = emptyList(),
    filter: HistoryFilter? = null,
    source: Source? = null,
    dismissedSuggestions: List<Long> = emptyList(),
    now: Instant = today(10),
  ): HistoryViewState = timeline(
    events = events,
    playing = playing,
    bookmarks = bookmarks,
    filter = filter,
    source = source,
    now = now,
  ).viewState(now = now, currentProgress = index.current?.progress, dismissedSuggestions = dismissedSuggestions)

  private fun HistoryViewState.entries(): List<HistoryEntry> = sessions.flatMap { it.entries }

  private fun location(
    chapterNumber: Int,
    time: Duration,
  ) = HistoryLocation(chapterNumber, chapterTime(time))

  @Test
  fun `a pause of 15 minutes starts a new session`() {
    val state = state(
      testEvent(Type.Play, today(7), signal, 0.minutes),
      testEvent(Type.Pause, today(7, 10), signal, 1.minutes),
      testEvent(Type.Play, today(7, 20), signal, 1.minutes),
      testEvent(Type.Pause, today(7, 30), signal, 2.minutes),
      testEvent(Type.Play, today(8), signal, 2.minutes),
      testEvent(Type.Pause, today(8, 20), signal, 4.minutes),
    )
    assertEquals(expected = listOf(LocalTime.of(8, 0), LocalTime.of(7, 0)), actual = state.sessions.map { it.start })
    assertEquals(expected = listOf(LocalTime.of(8, 20), LocalTime.of(7, 30)), actual = state.sessions.map { it.end })
    assertEquals(expected = listOf(20.minutes, 20.minutes), actual = state.sessions.map { it.listened })
    assertEquals(expected = listOf(DayLabel.ThisMorning, DayLabel.ThisMorning), actual = state.sessions.map { it.label })
  }

  @Test
  fun `listening for hours without a pause is one session`() {
    val state = state(
      testEvent(Type.Play, today(6), arrival, 0.minutes),
      testEvent(Type.ChapterChange, today(7), arrival, 10.minutes, to = signal to Duration.ZERO),
      testEvent(Type.ChapterChange, today(8), signal, 10.minutes, to = static to Duration.ZERO),
    )
    assertEquals(expected = 1, actual = state.sessions.size)
  }

  @Test
  fun `an ongoing session lasts until now and reaches the current position`() {
    val state = state(
      testEvent(Type.Play, today(9, 30), arrival, 0.minutes),
      playing = true,
    )
    val session = state.sessions.single()
    assertEquals(expected = LocalTime.of(10, 0), actual = session.end)
    assertEquals(expected = 30.minutes, actual = session.listened)
    assertEquals(expected = 0F, actual = session.barStart)
    assertEquals(expected = 0.5F, actual = session.barEnd)
  }

  @Test
  fun `newest entries come first`() {
    val state = state(
      testEvent(Type.Play, today(9), signal, 0.minutes),
      testEvent(Type.SpeedChanged, today(9, 5), signal, 1.minutes, value = "1.5"),
    )
    assertEquals(expected = listOf(Type.SpeedChanged, Type.Play), actual = state.entries().map { it.type })
  }

  @Test
  fun `skips close to each other merge into one row`() {
    val state = state(
      testEvent(Type.SkipForward, today(9), signal, 1.minutes, to = signal to 1.5.minutes, value = "30"),
      testEvent(Type.SkipForward, today(9).plusSeconds(20), signal, 1.5.minutes, to = signal to 2.minutes, value = "30"),
      testEvent(Type.SkipForward, today(9).plusSeconds(70), signal, 2.minutes, to = signal to 2.5.minutes, value = "30"),
      testEvent(Type.SkipForward, today(9, 5), signal, 2.5.minutes, to = signal to 3.minutes, value = "30"),
    )
    val (later, merged) = state.entries()
    assertEquals(expected = 3, actual = merged.count)
    assertEquals(expected = HistoryDetail.Skipped(90), actual = merged.detail)
    assertEquals(expected = location(2, 1.minutes), actual = merged.where)
    assertEquals(expected = location(2, 2.5.minutes), actual = merged.to)
    assertEquals(
      expected = HistoryAction.JumpBack(signal.id, 1.minutes.inWholeMilliseconds, location(2, 1.minutes)),
      actual = merged.action,
    )
    assertEquals(expected = 1, actual = later.count)
  }

  @Test
  fun `tiny seeks are hidden`() {
    val state = state(
      testEvent(Type.Seek, today(9), signal, 1.minutes, to = signal to 1.minutes + 3.seconds),
      testEvent(Type.Seek, today(9, 5), signal, 1.minutes, to = signal to 4.minutes),
    )
    assertEquals(expected = listOf(location(2, 4.minutes)), actual = state.entries().map { it.to })
  }

  @Test
  fun `a pause and the play that ends it are one row`() {
    val state = state(
      testEvent(Type.Pause, today(9), signal, 1.minutes),
      testEvent(Type.Play, today(9, 5), signal, 1.minutes),
      testEvent(Type.Pause, today(9, 6), signal, 2.minutes, source = Source.Headset),
      testEvent(Type.Play, today(9, 7), signal, 2.minutes, source = Source.Car),
    )
    assertEquals(
      expected = listOf(Type.Play, Type.Pause, Type.Pause),
      actual = state.entries().map { it.type },
    )
    assertEquals(
      expected = listOf(null, null, HistoryDetail.PausedFor(5.minutes)),
      actual = state.entries().map { it.detail },
    )
  }

  @Test
  fun `a lost pause does not merge sessions across days`() {
    val state = state(
      testEvent(Type.Play, yesterday(22), signal, 0.minutes),
      testEvent(Type.Play, today(8), signal, 1.minutes),
      testEvent(Type.Pause, today(8, 20), signal, 3.minutes),
    )
    assertEquals(expected = listOf(LocalTime.of(8, 0), LocalTime.of(22, 0)), actual = state.sessions.map { it.start })
    assertEquals(expected = listOf(LocalTime.of(8, 20), LocalTime.of(22, 0)), actual = state.sessions.map { it.end })
    assertEquals(expected = listOf(20.minutes, Duration.ZERO), actual = state.sessions.map { it.listened })
  }

  @Test
  fun `a lost pause within a session ends the listening at the event before`() {
    val state = state(
      testEvent(Type.Play, today(9), signal, 0.minutes),
      testEvent(Type.SpeedChanged, today(9, 5), signal, 5.minutes, value = "1.5"),
      testEvent(Type.Play, today(9, 10), signal, 5.minutes),
      testEvent(Type.Pause, today(9, 20), signal, 7.minutes),
    )
    assertEquals(expected = 15.minutes, actual = state.sessions.single().listened)
  }

  @Test
  fun `a session that was paused is not ongoing even while playing`() {
    val state = state(
      testEvent(Type.Play, today(9), arrival, 0.minutes),
      testEvent(Type.Pause, today(9, 20), arrival, 3.minutes),
      playing = true,
    )
    val session = state.sessions.single()
    assertEquals(expected = LocalTime.of(9, 20), actual = session.end)
    assertEquals(expected = 20.minutes, actual = session.listened)
    assertEquals(expected = 0.1F, actual = session.barEnd)
  }

  @Test
  fun `the ongoing session grows with time and position without building the timeline again`() {
    val timeline = timeline(testEvent(Type.Play, today(9, 30), arrival, 0.minutes), playing = true)
    assertTrue(timeline.isOngoing)
    val session = timeline.viewState(now = today(10, 15), currentProgress = 0.9F, dismissedSuggestions = emptyList())
      .sessions.single()
    assertEquals(expected = LocalTime.of(10, 15), actual = session.end)
    assertEquals(expected = 45.minutes, actual = session.listened)
    assertEquals(expected = 0.9F, actual = session.barEnd)
  }

  @Test
  fun `the last touch before falling asleep can be gone back to`() {
    val state = state(
      testEvent(Type.Play, today(8), arrival, 0.minutes),
      testEvent(Type.SleepTimerSet, today(8), arrival, 0.minutes, value = "20"),
      testEvent(Type.SkipForward, today(8, 10), arrival, 5.minutes, to = arrival to 5.5.minutes, source = Source.Headset, value = "30"),
      testEvent(Type.SleepTimerEnded, today(8, 20), arrival, 15.minutes, source = Source.SleepTimer),
    )
    val lastTouch = state.entries().single { it.lastTouch }
    assertEquals(expected = Type.SkipForward, actual = lastTouch.type)
    assertEquals(expected = HistoryAction.GoThere(arrival.id, 5.5.minutes.inWholeMilliseconds), actual = lastTouch.action)
    assertTrue(HistoryFilter.Sleep in lastTouch.filters())
  }

  @Test
  fun `only moving playback counts as the last touch`() {
    val bookmark = testBookmark(arrival, 2.minutes)
    val state = state(
      testEvent(Type.Play, today(8), arrival, 0.minutes, source = Source.Headset),
      testEvent(Type.SkipForward, today(8, 10), arrival, 5.minutes, to = arrival to 5.5.minutes, value = "30"),
      testEvent(Type.BookmarkDeleted, today(8, 12), arrival, 2.minutes, bookmark = bookmark),
      testEvent(Type.SkipSilenceChanged, today(8, 13), arrival, 6.minutes, value = "true"),
      testEvent(Type.SleepTimerEnded, today(8, 20), arrival, 15.minutes, source = Source.SleepTimer),
    )
    val (_, skipSilence, deleted, skip) = state.entries()
    assertTrue(skip.lastTouch)
    assertEquals(expected = HistoryAction.GoThere(arrival.id, 5.5.minutes.inWholeMilliseconds), actual = skip.action)
    assertEquals(expected = false, actual = deleted.lastTouch)
    assertIs<HistoryAction.Restore>(deleted.action)
    assertEquals(expected = false, actual = skipSilence.lastTouch)
    assertEquals(expected = HistoryAction.ChangeBack(Type.SkipSilenceChanged, "false"), actual = skipSilence.action)
  }

  @Test
  fun `playback the car started is suggested to undo`() {
    val play = testEvent(Type.Play, today(9, 30), signal, 5.minutes, source = Source.Car)
    val state = state(
      testEvent(Type.Pause, today(7), signal, 5.minutes),
      play,
      playing = true,
    )
    assertEquals(
      expected = HistorySuggestion.StartedBy(
        key = play.id,
        back = HistoryAction.JumpBack(signal.id, 5.minutes.inWholeMilliseconds, location(2, 5.minutes)),
        source = Source.Car,
        at = LocalTime.of(9, 30),
        played = 30.minutes,
      ),
      actual = state.suggestion,
    )
    assertEquals(expected = state.suggestion?.back, actual = state.entries().first().action)
  }

  @Test
  fun `touching the app after the car started playback means it was fine`() {
    val state = state(
      testEvent(Type.Play, today(9, 30), signal, 5.minutes, source = Source.Car),
      testEvent(Type.SpeedChanged, today(9, 40), signal, 6.minutes, value = "1.2"),
      playing = true,
    )
    assertNull(state.suggestion)
  }

  @Test
  fun `a short start is not worth a suggestion`() {
    val state = state(
      testEvent(Type.Play, today(9, 59).plusSeconds(30), signal, 5.minutes, source = Source.Bluetooth),
      playing = true,
    )
    assertNull(state.suggestion)
  }

  @Test
  fun `a big jump in the last minutes is suggested to undo`() {
    val jump = testEvent(Type.ChapterChange, today(9, 55), signal, 5.minutes, to = static to Duration.ZERO)
    val state = state(testEvent(Type.Play, today(9, 50), signal, 0.minutes), jump, playing = true)
    assertEquals(
      expected = HistorySuggestion.Jumped(
        key = jump.id,
        back = HistoryAction.JumpBack(signal.id, 5.minutes.inWholeMilliseconds, location(2, 5.minutes)),
        at = LocalTime.of(9, 55),
      ),
      actual = state.suggestion,
    )
    assertNull(state(jump, now = today(10, 6)).suggestion)
    assertNull(state(jump, dismissedSuggestions = listOf(jump.id)).suggestion)
  }

  @Test
  fun `undoing merged jumps goes back to before the first of them`() {
    val first = testEvent(Type.Seek, today(9, 55), signal, 1.minutes, to = signal to 3.minutes)
    val state = state(
      testEvent(Type.Play, today(9, 50), signal, 0.minutes),
      first,
      testEvent(Type.Seek, today(9, 55).plusSeconds(20), signal, 3.minutes, to = signal to 5.minutes),
      testEvent(Type.Seek, today(9, 55).plusSeconds(40), signal, 5.minutes, to = signal to 7.minutes),
      playing = true,
    )
    val back = HistoryAction.JumpBack(signal.id, 1.minutes.inWholeMilliseconds, location(2, 1.minutes))
    assertEquals(
      expected = HistorySuggestion.Jumped(key = first.id, back = back, at = LocalTime.of(9, 55)),
      actual = state.suggestion,
    )
    val moved = state.entries().first()
    assertEquals(expected = 3, actual = moved.count)
    assertEquals(expected = back, actual = moved.action)
  }

  @Test
  fun `a jump that was already undone is not suggested`() {
    val state = state(
      testEvent(Type.Seek, today(9, 55), signal, 5.minutes, to = signal to 9.minutes),
      testEvent(Type.JumpBack, today(9, 56), signal, 9.minutes, to = signal to 5.minutes),
    )
    assertNull(state.suggestion)
    assertEquals(
      expected = HistoryAction.Pin(signal.id, 5.minutes.inWholeMilliseconds),
      actual = state.entries().first().action,
    )
  }

  @Test
  fun `filters only offer what happened and narrow the rows`() {
    val events = arrayOf(
      testEvent(Type.Play, today(9), signal, 0.minutes, source = Source.Headset),
      testEvent(Type.Seek, today(9, 1), signal, 0.minutes, to = signal to 3.minutes),
      testEvent(Type.SpeedChanged, today(9, 2), signal, 3.minutes, value = "1.5"),
    )
    val all = state(*events)
    assertEquals(
      expected = listOf(HistoryFilter.Jumps, HistoryFilter.PlayPause, HistoryFilter.Settings),
      actual = all.filters,
    )
    assertEquals(expected = listOf(Source.App, Source.Headset), actual = all.sources)

    val settings = state(*events, filter = HistoryFilter.Settings)
    assertEquals(expected = listOf(Type.SpeedChanged), actual = settings.entries().map { it.type })

    val headset = state(*events, source = Source.Headset)
    assertEquals(expected = listOf(Type.Play), actual = headset.entries().map { it.type })

    val unavailable = state(*events, filter = HistoryFilter.Sleep)
    assertNull(unavailable.selectedFilter)
    assertEquals(expected = 3, actual = unavailable.entries().size)
  }

  @Test
  fun `a deleted bookmark can be restored until it is back`() {
    val bookmark = testBookmark(signal, 4.minutes, addedAt = yesterday(20), title = "Mira's message", kind = Bookmark.Kind.Quote)
    val deleted = testEvent(Type.BookmarkDeleted, today(9), signal, 4.minutes, value = bookmark.title, bookmark = bookmark)

    val restore = assertIs<HistoryAction.Restore>(state(deleted).entries().single().action)
    assertEquals(expected = bookmark, actual = restore.bookmark)

    assertNull(state(deleted, bookmarks = listOf(bookmark)).entries().single().action)
  }

  @Test
  fun `a bookmark deleted without its date is restored with the date it was deleted`() {
    val bookmark = testBookmark(signal, 4.minutes, addedAt = yesterday(20))
    val deleted = testEvent(Type.BookmarkDeleted, today(9), signal, 4.minutes, bookmark = bookmark)
      .copy(bookmarkAddedAtMillis = null)

    val restore = assertIs<HistoryAction.Restore>(state(deleted).entries().single().action)
    assertEquals(expected = bookmark.copy(addedAt = deleted.at), actual = restore.bookmark)
  }

  @Test
  fun `bookmark rows show the bookmark as it is now`() {
    val bookmark = testBookmark(signal, 4.minutes, title = "Mira's message", kind = Bookmark.Kind.Quote)
    val added = testEvent(
      Type.BookmarkAdded,
      today(9),
      signal,
      4.minutes,
      bookmark = bookmark.copy(title = null, kind = Bookmark.Kind.Note),
    )

    assertEquals(
      expected = HistoryDetail.BookmarkInfo(kind = Bookmark.Kind.Quote, setBySleepTimer = false, note = "Mira's message"),
      actual = state(added, bookmarks = listOf(bookmark)).entries().single().detail,
    )
    assertEquals(
      expected = HistoryDetail.BookmarkInfo(kind = Bookmark.Kind.Note, setBySleepTimer = false, note = null),
      actual = state(added).entries().single().detail,
    )
  }

  @Test
  fun `a spot that already has a bookmark is not offered to pin`() {
    val pause = testEvent(Type.Pause, today(9), signal, 2.minutes)
    assertEquals(
      expected = HistoryAction.Pin(signal.id, 2.minutes.inWholeMilliseconds),
      actual = state(pause).entries().single().action,
    )
    val bookmark = testBookmark(signal, 2.minutes)
    assertNull(state(pause, bookmarks = listOf(bookmark)).entries().single().action)
  }

  @Test
  fun `spots in chapters that are gone after a rescan offer nothing to do`() {
    val gone = testChapter()
    val bookmark = testBookmark(gone, 4.minutes)
    val state = state(
      testEvent(Type.Play, today(8), gone, 0.minutes),
      testEvent(Type.SkipForward, today(8, 10), gone, 5.minutes, to = gone to 5.5.minutes, value = "30"),
      testEvent(Type.BookmarkDeleted, today(8, 12), gone, 4.minutes, bookmark = bookmark),
      testEvent(Type.Pause, today(8, 15), gone, 6.minutes),
      testEvent(Type.Play, today(8, 30), gone, 6.minutes),
      testEvent(Type.SleepTimerEnded, today(8, 40), gone, 10.minutes, source = Source.SleepTimer),
    )
    assertEquals(
      expected = listOf(Type.SleepTimerEnded, Type.Play, Type.Pause, Type.BookmarkDeleted, Type.SkipForward, Type.Play),
      actual = state.entries().map { it.type },
    )
    assertEquals(expected = List(6) { null }, actual = state.entries().map { it.action })
    assertEquals(expected = listOf(false, true, false, false, false, false), actual = state.entries().map { it.lastTouch })
  }

  @Test
  fun `settings can be changed back to what they were`() {
    val state = state(
      testEvent(Type.SpeedChanged, today(9), signal, 0.minutes, value = "1.0"),
      testEvent(Type.SpeedChanged, today(9, 5), signal, 0.minutes, value = "1.8"),
      testEvent(Type.SkipSilenceChanged, today(9, 6), signal, 0.minutes, value = "true"),
    )
    val (skipSilence, faster, first) = state.entries()
    assertEquals(expected = HistoryAction.ChangeBack(Type.SkipSilenceChanged, "false"), actual = skipSilence.action)
    assertEquals(expected = HistoryDetail.Speed(from = 1F, to = 1.8F), actual = faster.detail)
    assertEquals(expected = HistoryAction.ChangeBack(Type.SpeedChanged, "1.0"), actual = faster.action)
    assertEquals(expected = HistoryDetail.Speed(from = null, to = 1F), actual = first.detail)
    assertNull(first.action)
  }
}

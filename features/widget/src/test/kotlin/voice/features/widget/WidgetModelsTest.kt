package voice.features.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import voice.core.data.MarkData
import voice.core.sleeptimer.SleepTimerState
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WidgetModelsTest {

  private val now = Instant.parse("2026-10-08T22:00:00Z")

  @Test
  fun `chapter number counts the marks of earlier chapters`() {
    val marked = chapter(
      duration = 20.minutes.inWholeMilliseconds,
      marks = listOf(MarkData(0, "Loomings"), MarkData(10.minutes.inWholeMilliseconds, "The Carpet-Bag")),
    )
    val second = chapter(name = "The Spouter-Inn")
    val third = chapter(name = "The Counterpane")
    val book = book(chapters = listOf(marked, second, third), currentChapter = third.id)

    val nowPlaying = nowPlayingBook(book, playing = false, SleepTimerState.Disabled, now)

    assertEquals(4, nowPlaying.chapterNumber)
    assertEquals(4, nowPlaying.chapterCount)
    assertEquals("The Counterpane", nowPlaying.chapterName)
  }

  @Test
  fun `a book with a single chapter has no chapter name`() {
    val book = book(chapters = listOf(chapter(name = "Moby-Dick")))

    assertNull(nowPlayingBook(book, playing = false, SleepTimerState.Disabled, now).chapterName)
  }

  @Test
  fun `time left is at the playback speed and the position only counts in whole minutes`() {
    val first = chapter(duration = 30.minutes.inWholeMilliseconds)
    val second = chapter(duration = 30.minutes.inWholeMilliseconds)
    val book = book(
      chapters = listOf(first, second),
      currentChapter = second.id,
      positionInChapter = (10.minutes + 20.seconds).inWholeMilliseconds,
      speed = 2F,
    )

    val nowPlaying = nowPlayingBook(book, playing = true, SleepTimerState.Disabled, now)

    assertEquals(10.minutes, nowPlaying.remaining)
    assertEquals(40F / 60F, nowPlaying.progress)
    assertEquals(60.minutes, nowPlaying.duration)
  }

  @Test
  fun `time left rounds up to whole minutes`() {
    val single = chapter(duration = (2.minutes + 30.seconds).inWholeMilliseconds)
    val book = book(chapters = listOf(single))

    assertEquals(3.minutes, nowPlayingBook(book, playing = false, SleepTimerState.Disabled, now).remaining)
  }

  @Test
  fun `a book within five seconds of its end is finished`() {
    val single = chapter(duration = 10.minutes.inWholeMilliseconds)

    val almost = book(chapters = listOf(single), positionInChapter = (10.minutes - 6.seconds).inWholeMilliseconds)
    val finished = book(chapters = listOf(single), positionInChapter = (10.minutes - 4.seconds).inWholeMilliseconds)

    assertFalse(nowPlayingBook(almost, playing = false, SleepTimerState.Disabled, now).finished)
    assertTrue(nowPlayingBook(finished, playing = false, SleepTimerState.Disabled, now).finished)
  }

  @Test
  fun `a timed sleep timer ends at a clock time rounded to the minute`() {
    val end = sleepTimerEnd(SleepTimerState.Enabled.WithDuration(20.minutes + 20.seconds), book = null, now)

    assertEquals(SleepTimerEnd(at = Instant.parse("2026-10-08T22:20:00Z"), endOfChapter = false), end)
  }

  @Test
  fun `an end of chapter sleep timer ends with the chapter at the playback speed`() {
    val single = chapter(duration = 30.minutes.inWholeMilliseconds)
    val book = book(chapters = listOf(single), positionInChapter = 10.minutes.inWholeMilliseconds, speed = 2F)

    val end = sleepTimerEnd(SleepTimerState.Enabled.WithEndOfChapter, book, now)

    assertEquals(SleepTimerEnd(at = Instant.parse("2026-10-08T22:10:00Z"), endOfChapter = true), end)
  }

  @Test
  fun `an end of chapter sleep timer without a book has no end time`() {
    val end = sleepTimerEnd(SleepTimerState.Enabled.WithEndOfChapter, book = null, now)

    assertEquals(SleepTimerEnd(at = null, endOfChapter = true), end)
  }

  @Test
  fun `the sleep timer widget offers the default duration while the timer is off`() {
    val model = sleepTimerWidgetModel(SleepTimerState.Disabled, defaultDuration = 20.minutes, book = null, now)

    assertEquals(SleepTimerWidgetModel.Ready(20.minutes), model)
  }

  @Test
  fun `the shelf shows started books, the current one first and then the most recently played`() {
    val started = book(positionInChapter = 1_000, lastPlayedAt = Instant.ofEpochSecond(3))
    val recent = book(positionInChapter = 1_000, lastPlayedAt = Instant.ofEpochSecond(5))
    val notStarted = book(lastPlayedAt = Instant.ofEpochSecond(9))
    val single = chapter()
    val finished = book(chapters = listOf(single), positionInChapter = single.duration, lastPlayedAt = Instant.ofEpochSecond(8))
    val current = book(lastPlayedAt = Instant.ofEpochSecond(1))

    val shelf = shelfBooks(listOf(started, recent, notStarted, finished, current), currentBookId = current.id, playing = true)

    assertEquals(listOf(current.id, recent.id, started.id), shelf.map { it.id })
    assertEquals(listOf(true, false, false), shelf.map { it.playing })
  }

  @Test
  fun `the shelf holds at most five books`() {
    val books = List(8) { book(positionInChapter = 1_000) }

    assertEquals(SHELF_MAX_BOOKS, shelfBooks(books, currentBookId = null, playing = false).size)
  }

  @Test
  fun `the now playing layout follows the widget size`() {
    assertEquals(NowPlayingLayout.Cover, nowPlayingLayout(DpSize(80.dp, 80.dp)))
    assertEquals(NowPlayingLayout.Cover, nowPlayingLayout(DpSize(80.dp, 200.dp)))
    assertEquals(NowPlayingLayout.Bar, nowPlayingLayout(DpSize(180.dp, 90.dp)))
    assertEquals(NowPlayingLayout.Strip, nowPlayingLayout(DpSize(320.dp, 90.dp)))
    assertEquals(NowPlayingLayout.Art, nowPlayingLayout(DpSize(160.dp, 160.dp)))
    assertEquals(NowPlayingLayout.Hero, nowPlayingLayout(DpSize(320.dp, 180.dp)))
    assertEquals(NowPlayingLayout.HeroTall, nowPlayingLayout(DpSize(320.dp, 300.dp)))
  }
}

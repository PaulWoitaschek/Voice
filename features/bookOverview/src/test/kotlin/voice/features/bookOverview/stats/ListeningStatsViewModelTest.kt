package voice.features.bookOverview.stats

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.supporter.SupporterBadge
import voice.core.data.supporter.SupporterStatus
import voice.core.data.supporter.SupporterStatusStore
import voice.features.bookOverview.MemoryDataStore
import voice.features.bookOverview.book
import voice.navigation.Destination
import voice.navigation.Navigator

class ListeningStatsViewModelTest {

  // Friday, 20 November 2026. The week goes from Monday the 16th to Sunday the 22nd.
  private var now = Instant.parse("2026-11-20T12:00:00Z")
  private val monday = LocalDate.of(2026, 11, 16)
  private val sunday = LocalDate.of(2026, 11, 22)
  private val today = LocalDate.of(2026, 11, 20)

  private val sessions = MutableStateFlow<List<ListeningSession>>(emptyList())
  private val books = MutableStateFlow<List<Book>>(emptyList())
  private val contents = MutableStateFlow<List<BookContent>>(emptyList())
  private val supporterStatus = MemoryDataStore(SupporterStatus())
  private val navigator = mockk<Navigator>(relaxUnitFun = true)
  private val analytics = mockk<Analytics>(relaxUnitFun = true)

  private val viewModel by lazy {
    val statsRepo =
      mockk<ListeningStatsRepo> {
        every { sessions() } returns sessions
      }
    ListeningStatsViewModel(
      statsRepo = statsRepo,
      bookRepo =
        mockk<BookRepository> {
          every { flow() } returns books
        },
      contentRepo =
        mockk<BookContentRepo> {
          every { flow() } returns contents
        },
      supporterStatusStore = SupporterStatusStore(supporterStatus),
      weekListening =
        WeekListening(
          repo = statsRepo,
          clock =
            object : Clock {
              override fun now() = now
            },
          zone = { ZoneOffset.UTC },
          firstDayOfWeek = { DayOfWeek.MONDAY },
        ),
      zone = { ZoneOffset.UTC },
      navigator = navigator,
      analytics = analytics,
    )
  }

  @Test
  fun `library, tap the line, the screen opens on this week`() = runTest {
    sessions.value = listOf(session(today.atHour(8), 1.hours))

    val state = state()

    assertEquals(expected = PeriodKind.Week, actual = state.period.kind)
    assertEquals(expected = monday, actual = state.period.start)
    assertTrue(state.current)
  }

  @Test
  fun `week, 2 h on Monday and 1 h on Friday, open the screen, 3 h, on 2 of 7 days, a bar on Monday and on Friday`() =
    runTest {
      sessions.value = listOf(session(monday.atHour(9), 2.hours), session(today.atHour(8), 1.hours))

      val state = state()

      assertEquals(expected = 3.hours, actual = state.listened)
      assertEquals(expected = 2, actual = state.days)
      assertEquals(expected = 7, actual = state.period.days)
      assertEquals(
        expected =
          listOf(
            2.hours,
            Duration.ZERO,
            Duration.ZERO,
            Duration.ZERO,
            1.hours,
            Duration.ZERO,
            Duration.ZERO,
          ),
        actual = state.bars,
      )
    }

  @Test
  fun `1 h listened at 1,5x, open the screen, 1 h listened, 30 min saved`() = runTest {
    sessions.value = listOf(session(today.atHour(8), 1.hours, audio = 90.minutes))

    val state = state()

    assertEquals(expected = 1.hours, actual = state.listened)
    assertEquals(expected = 30.minutes, actual = state.saved)
  }

  @Test
  fun `only 1x in the period, open the screen, no saved by listening faster`() = runTest {
    sessions.value = listOf(session(today.atHour(8), 1.hours))

    assertNull(state().saved)
  }

  @Test
  fun `a stretch from 23_30 to 0_30, open the week, the hour counts on the first day`() = runTest {
    // 23:30 in Berlin in winter is 22:30 in UTC
    sessions.value =
      listOf(session(Instant.parse("2026-11-17T22:30:00Z"), 1.hours, utcOffset = 1.hours))

    val state = state()

    assertEquals(expected = 1.hours, actual = state.bars[1])
    assertEquals(expected = Duration.ZERO, actual = state.bars[2])
    assertEquals(expected = 1, actual = state.days)
  }

  @Test
  fun `this week open on Sunday night, midnight passes, the new week`() = runTest {
    now = Instant.parse("2026-11-22T23:59:30Z")

    states {
      assertEquals(expected = monday, actual = awaitItem().period.start)
      now = Instant.parse("2026-11-23T00:00:30Z")

      assertEquals(expected = monday.plusWeeks(1), actual = awaitItem().period.start)
    }
  }

  @Test
  fun `this week, go back one week, the week before, with its dates`() = runTest {
    sessions.value = listOf(session(monday.minusDays(10).atHour(9), 1.hours))

    states {
      assertTrue(awaitItem().current)
      viewModel.onEarlierClick()

      val state = awaitItem()
      assertFalse(state.current)
      assertEquals(expected = monday.minusWeeks(1), actual = state.period.start)
      assertEquals(expected = monday, actual = state.period.end)
      assertTrue(state.canGoForward)
    }
  }

  @Test
  fun `the first week with listening, go back, no way back`() = runTest {
    sessions.value = listOf(session(monday.minusDays(3).atHour(9), 1.hours))

    states {
      assertTrue(awaitItem().canGoBack)
      viewModel.onEarlierClick()

      assertFalse(awaitItem().canGoBack)
    }
  }

  @Test
  fun `this week, go forward, no way forward`() = runTest {
    sessions.value = listOf(session(today.atHour(8), 1.hours))

    states {
      assertFalse(awaitItem().canGoForward)
      viewModel.onLaterClick()

      expectNoEvents()
    }
  }

  @Test
  fun `year, pick the chip, this year, with one bar per month`() = runTest {
    sessions.value =
      listOf(
        session(LocalDate.of(2026, 3, 2).atHour(9), 1.hours),
        session(today.atHour(8), 2.hours),
      )

    states {
      awaitItem()
      viewModel.onKindClick(PeriodKind.Year)

      val state = awaitItem()
      assertTrue(state.current)
      assertEquals(expected = LocalDate.of(2026, 1, 1), actual = state.period.start)
      assertEquals(expected = 12, actual = state.bars.size)
      assertEquals(expected = 1.hours, actual = state.bars[2])
      assertEquals(expected = 2.hours, actual = state.bars[10])
    }
  }

  @Test
  fun `all time, pick the chip, all the time, the days, the day Voice counts from, no chart`() =
    runTest {
      sessions.value =
        listOf(
          session(LocalDate.of(2026, 10, 8).atHour(9), 1.hours),
          session(LocalDate.of(2026, 10, 9).atHour(9), 1.hours),
          session(today.atHour(8), 2.hours),
        )

      states {
        awaitItem()
        viewModel.onKindClick(PeriodKind.AllTime)

        val state = awaitItem()
        assertEquals(expected = 4.hours, actual = state.listened)
        assertEquals(expected = 3, actual = state.days)
        assertEquals(expected = LocalDate.of(2026, 10, 8), actual = state.countsSince)
        assertEquals(expected = emptyList(), actual = state.bars)
        assertFalse(state.canGoBack)
        assertFalse(state.canGoForward)
      }
    }

  @Test
  fun `a week without listening, open it, nothing here yet`() = runTest {
    sessions.value = listOf(session(monday.minusDays(10).atHour(9), 1.hours))

    assertTrue(state().empty)
  }

  @Test
  fun `a book plays to its end on Sunday, open the week, the book is under Finished with Sunday`() =
    runTest {
      val book = book()
      books.value = listOf(book)
      contents.value = listOf(book.content)
      sessions.value =
        listOf(session(sunday.atHour(9), 1.hours, bookId = book.id, reachedEnd = true))

      assertEquals(
        expected = listOf(book.id to sunday),
        actual = state().finished.map { it.id to it.day },
      )
    }

  @Test
  fun `a book in Current, 2 minutes before its end, mark as completed, the book is under Finished with today`() =
    runTest {
      val book = marked(startedBeforeMark = true)

      assertEquals(
        expected = listOf(book.id to today),
        actual = state().finished.map { it.id to it.day },
      )
    }

  @Test
  fun `a book marked as completed, rename its folder, under Finished once, and gone after mark as current`() =
    runTest {
      val old = marked(startedBeforeMark = true)
      val renamed = book().update { it.copy(markedCompletedAt = now, startedBeforeMark = true) }
      books.value = listOf(renamed)
      contents.value = listOf(old.content.copy(isActive = false), renamed.content)

      assertEquals(expected = listOf(renamed.id), actual = state().finished.map { it.id })

      contents.value =
        listOf(old.content.copy(isActive = false), renamed.content.copy(markedCompletedAt = null))

      assertEquals(expected = emptyList(), actual = state().finished)
    }

  @Test
  fun `a book not started yet, mark as completed, not under Finished`() = runTest {
    val _ = marked(startedBeforeMark = false)

    assertEquals(expected = emptyList(), actual = state().finished)
  }

  @Test
  fun `a series with 1 book in progress and 5 not started, mark the series as completed, 1 book under Finished`() =
    runTest {
      val started = marked(startedBeforeMark = true)
      val notStarted = List(5) { book().markedNow(startedBeforeMark = false) }
      books.value += notStarted
      contents.value += notStarted.map { it.content }

      assertEquals(expected = listOf(started.id), actual = state().finished.map { it.id })
    }

  @Test
  fun `a book marked as completed today, mark as current, no longer under Finished`() = runTest {
    val book = marked(startedBeforeMark = true)

    states {
      assertEquals(expected = 1, actual = awaitItem().finished.size)
      // what a mark as current leaves behind
      contents.value =
        listOf(book.content.copy(markedCompletedAt = null, startedBeforeMark = false))

      assertEquals(expected = emptyList(), actual = awaitItem().finished)
    }
  }

  @Test
  fun `a book that played to its end in March, mark as not started yet, and play it again, still under Finished in March`() =
    runTest {
      val book = book()
      val march = LocalDate.of(2026, 3, 14)
      books.value = listOf(book)
      contents.value = listOf(book.content)
      sessions.value =
        listOf(
          session(march.atHour(9), 1.hours, bookId = book.id, reachedEnd = true),
          session(today.atHour(8), 1.hours, bookId = book.id),
        )

      states {
        awaitItem()
        viewModel.onKindClick(PeriodKind.Year)

        assertEquals(
          expected = listOf(book.id to march),
          actual = awaitItem().finished.map { it.id to it.day },
        )
      }
    }

  @Test
  fun `a book finished in March and again in June, open the year, one entry, with the day in June`() =
    runTest {
      val book = book()
      val june = LocalDate.of(2026, 6, 3)
      books.value = listOf(book)
      contents.value = listOf(book.content)
      sessions.value =
        listOf(
          session(
            LocalDate.of(2026, 3, 14).atHour(9),
            1.hours,
            bookId = book.id,
            reachedEnd = true,
          ),
          session(june.atHour(9), 1.hours, bookId = book.id, reachedEnd = true),
        )

      states {
        awaitItem()
        viewModel.onKindClick(PeriodKind.Year)

        assertEquals(
          expected = listOf(book.id to june),
          actual = awaitItem().finished.map { it.id to it.day },
        )
      }
    }

  @Test
  fun `a book marked as completed before Voice kept the day, open All time, not under Finished`() =
    runTest {
      val book = book()
      books.value = listOf(book)
      contents.value =
        listOf(book.content.copy(markedCompletedAt = null, startedBeforeMark = false))
      sessions.value = listOf(session(today.atHour(8), 1.hours))

      states {
        awaitItem()
        viewModel.onKindClick(PeriodKind.AllTime)

        assertEquals(expected = emptyList(), actual = awaitItem().finished)
      }
    }

  @Test
  fun `a finished book, later hidden, open the screen, under Finished, and a tap opens it`() =
    runTest {
      val book = book(hidden = true)
      books.value = listOf(book)
      contents.value = listOf(book.content)
      sessions.value =
        listOf(session(today.atHour(8), 1.hours, bookId = book.id, reachedEnd = true))

      val finished = state().finished.single()
      assertTrue(finished.inLibrary)

      viewModel.onBookClick(finished.id)
      verify { navigator.goTo(Destination.Playback(book.id)) }
    }

  @Test
  fun `a finished book, later removed with its files, open the screen, its name without a cover, no tap, and its time counts`() =
    runTest {
      val book = book(name = "Dune")
      contents.value = listOf(book.content.copy(isActive = false))
      sessions.value =
        listOf(session(today.atHour(8), 1.hours, bookId = book.id, reachedEnd = true))

      val state = state()
      val finished = state.finished.single()
      assertEquals(expected = "Dune", actual = finished.name)
      assertNull(finished.cover)
      assertFalse(finished.inLibrary)
      assertEquals(expected = 1.hours, actual = state.listened)
    }

  @Test
  fun `a finished book, tap it, the book opens`() = runTest {
    val id = BookId("dune")

    viewModel.onBookClick(id)

    verify(exactly = 1) { navigator.goTo(Destination.Playback(id)) }
  }

  @Test
  fun `the screen, open it, with analytics on, the screen and the kind of period are reported, no numbers, no books`() =
    runTest {
      sessions.value = listOf(session(today.atHour(8), 1.hours))

      states {
        awaitItem()
        viewModel.onKindClick(PeriodKind.Year)
        awaitItem()
      }

      verify(exactly = 1) {
        analytics.event("listening_stats_period", mapOf("period" to "week"))
        analytics.event("listening_stats_period", mapOf("period" to "year"))
      }
      verify(exactly = 2) { analytics.event(any(), any()) }
    }

  @Test
  fun `the support line, tap it, the support screen opens`() {
    viewModel.onSupportClick()

    verify(exactly = 1) { navigator.goTo(Destination.SupportVoice) }
  }

  @Test
  fun `the support line, with analytics on, tap it, the support screen is reported as opened from this screen`() {
    viewModel.onSupportClick()

    verify(exactly = 1) {
      analytics.event("support_open", mapOf("from" to "listening_stats"))
    }
  }

  @Test
  fun `a supporter, scroll to the end, their badge`() = runTest {
    supporterStatus.updateData { SupporterStatus(subscribedMonths = 4) }
    sessions.value = listOf(session(today.atHour(8), 1.hours))

    assertEquals(
      expected = SupporterBadge.ThreeMonths,
      actual = state().supporterBadge,
    )
  }

  private suspend fun TestScope.state(): ListeningStatsViewState {
    var state: ListeningStatsViewState? = null
    states { state = awaitItem() }
    return state!!
  }

  private suspend fun TestScope.states(
    validate: suspend TurbineTestContext<ListeningStatsViewState>.() -> Unit,
  ) {
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) { viewModel.viewState() }
      .filterNotNull()
      .test {
        validate()
        cancelAndIgnoreRemainingEvents()
      }
  }

  private fun marked(startedBeforeMark: Boolean): Book {
    val book = book().markedNow(startedBeforeMark)
    books.value = listOf(book)
    contents.value = listOf(book.content)
    return book
  }

  private fun Book.markedNow(startedBeforeMark: Boolean): Book = update {
    it.copy(markedCompletedAt = now, startedBeforeMark = startedBeforeMark)
  }

  private fun LocalDate.atHour(hour: Int): Instant =
    Instant.fromEpochSeconds(atTime(hour, 0).toEpochSecond(ZoneOffset.UTC))

  private fun session(
    startedAt: Instant,
    listened: Duration,
    audio: Duration = listened,
    bookId: BookId = BookId("book"),
    reachedEnd: Boolean = false,
    utcOffset: Duration = Duration.ZERO,
  ) =
    ListeningSession(
      bookId = bookId,
      startedAtMillis = startedAt.toEpochMilliseconds(),
      endedAtMillis = (startedAt + listened).toEpochMilliseconds(),
      listenedMillis = listened.inWholeMilliseconds,
      audioMillis = audio.inWholeMilliseconds,
      utcOffsetSeconds = utcOffset.inWholeSeconds.toInt(),
      reachedEnd = reachedEnd,
    )
}

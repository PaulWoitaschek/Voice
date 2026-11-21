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
import java.time.Month
import java.time.ZoneOffset
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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
import voice.features.bookOverview.chapter
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
    assertContains(state.smallFacts, SmallFact.Saved(30.minutes, speed = 1.5F))
  }

  @Test
  fun `only 1x in the period, open the screen, no saved by listening faster`() = runTest {
    sessions.value = listOf(session(today.atHour(8), 1.hours))

    assertEquals(
      expected = listOf<SmallFact>(SmallFact.LongestStretch(1.hours)),
      actual = state().smallFacts,
    )
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
        actual = state().finished.map { it.book.id to it.day },
      )
    }

  @Test
  fun `a book in Current, 2 minutes before its end, mark as completed, the book is under Finished with today`() =
    runTest {
      val book = marked(startedBeforeMark = true)

      assertEquals(
        expected = listOf(book.id to today),
        actual = state().finished.map { it.book.id to it.day },
      )
    }

  @Test
  fun `a book marked as completed, rename its folder, under Finished once, and gone after mark as current`() =
    runTest {
      val old = marked(startedBeforeMark = true)
      val renamed = book().update { it.copy(markedCompletedAt = now, startedBeforeMark = true) }
      books.value = listOf(renamed)
      contents.value = listOf(old.content.copy(isActive = false), renamed.content)

      assertEquals(expected = listOf(renamed.id), actual = state().finished.map { it.book.id })

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

      assertEquals(expected = listOf(started.id), actual = state().finished.map { it.book.id })
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
          actual = awaitItem().finished.map { it.book.id to it.day },
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
          actual = awaitItem().finished.map { it.book.id to it.day },
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

      val finished = state().finished.single().book
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
      val finished = state.finished.single().book
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

  @Test
  fun `a week with 5 h, 70 percent of it between 22_00 and 5_00, open the week, You are a night owl, 70 percent of your listening is at night`() =
    runTest {
      sessions.value =
        listening(monday, 22, 105.minutes) +
          listening(monday.plusDays(1), 22, 105.minutes) +
          listening(monday.plusDays(2), 12, 90.minutes)

      assertEquals(expected = ListenerName.NightOwl(percent = 70), actual = state().name)
    }

  @Test
  fun `a week with 5 h, 45 percent of it between 5_00 and 9_00, open the week, You are an early bird`() =
    runTest {
      sessions.value =
        listening(monday, 6, 75.minutes) +
          listening(monday.plusDays(1), 6, 1.hours) +
          listening(monday.plusDays(2), 12, 165.minutes)

      assertEquals(expected = ListenerName.EarlyBird(percent = 45), actual = state().name)
    }

  @Test
  fun `a week with 14 h, open the week, You are an audiomaniac, 2h on an average day`() = runTest {
    at(sunday, 12)
    sessions.value = List(5) { listening(monday.plusDays(it.toLong()), 10, 168.minutes) }.flatten()

    assertEquals(expected = ListenerName.Audiomaniac(perDay = 2.hours), actual = state().name)
  }

  @Test
  fun `a week with one stretch of 3 h 10 min, open the week, You are a marathon listener, 3h 10m in one go`() =
    runTest {
      sessions.value = listOf(session(monday.atHour(10), 190.minutes))

      assertEquals(
        expected = ListenerName.MarathonListener(stretch = 190.minutes),
        actual = state().name,
      )
    }

  @Test
  fun `a week with listening only on Monday to Friday, half at 7_00 and half at 17_00, open the week, You are a commuter`() =
    runTest {
      at(sunday.minusDays(1), 12)
      sessions.value =
        List(5) {
            val day = monday.plusDays(it.toLong())
            listening(day, 7, 30.minutes) + listening(day, 17, 30.minutes)
          }
          .flatten()

      // half at 7:00 also makes an early bird, so each of the two names has its day
      assertEquals(
        expected = setOf(ListenerName.EarlyBird(percent = 50), ListenerName.Commuter),
        actual = setOf(state().name, stateTomorrow().name),
      )
    }

  @Test
  fun `a week with 5 h on Saturday, and 10 min on Monday at 8_00 and on Wednesday at 17_00, open the week, no commuter`() =
    runTest {
      val saturday = monday.plusDays(5)
      at(saturday, 20)
      sessions.value =
        listening(monday, 8, 10.minutes) +
          listening(monday.plusDays(2), 17, 10.minutes) +
          listening(saturday, 12, 5.hours)

      assertEquals(
        expected = ListenerName.PartOfDay(DayPart.Afternoon, percent = 94),
        actual = state().name,
      )
    }

  @Test
  fun `a week with 3 books, 1 h each, open the week, You are a juggler, 3 books at the same time`() =
    runTest {
      sessions.value =
        List(3) { listening(monday.plusDays(it.toLong()), 12, 1.hours, BookId("book$it")) }
          .flatten()

      assertEquals(expected = ListenerName.Juggler(books = 3), actual = state().name)
    }

  @Test
  fun `a year with 3 books, 1 h each, open the year, no juggler`() = runTest {
    sessions.value =
      List(3) { listening(LocalDate.of(2026, 3 + it, 2), 12, 1.hours, BookId("book$it")) }.flatten()
    viewModel.onKindClick(PeriodKind.Year)

    assertEquals(
      expected = ListenerName.PartOfDay(DayPart.Afternoon, percent = 100),
      actual = state().name,
    )
  }

  @Test
  fun `a week with 2 books of 1 h and a book of 20 min, open the week, no juggler`() = runTest {
    sessions.value =
      listening(monday, 12, 1.hours, BookId("a")) +
        listening(monday.plusDays(1), 12, 1.hours, BookId("b")) +
        listening(monday.plusDays(2), 12, 20.minutes, BookId("c"))

    assertEquals(
      expected = ListenerName.PartOfDay(DayPart.Afternoon, percent = 100),
      actual = state().name,
    )
  }

  @Test
  fun `a week with 4 h at 1,5x, open the week, You are a speedster, You listen at 1,5x`() =
    runTest {
      sessions.value =
        listening(monday, 12, 2.hours, speed = 1.5) +
          listening(monday.plusDays(1), 12, 2.hours, speed = 1.5)

      assertEquals(expected = ListenerName.Speedster(speed = 1.5F), actual = state().name)
    }

  @Test
  fun `a month with 10 h, 7 h of it on Saturdays and Sundays, open the month, You are a weekend listener`() =
    runTest {
      val november = LocalDate.of(2026, 11, 1)
      sessions.value =
        listening(november.withDayOfMonth(3), 12, 90.minutes) +
          listening(november.withDayOfMonth(7), 12, 2.hours) +
          listening(november.withDayOfMonth(8), 12, 2.hours) +
          listening(november.withDayOfMonth(10), 12, 90.minutes) +
          listening(november.withDayOfMonth(14), 12, 90.minutes) +
          listening(november.withDayOfMonth(15), 12, 90.minutes)
      viewModel.onKindClick(PeriodKind.Month)

      assertEquals(expected = ListenerName.WeekendListener(percent = 70), actual = state().name)
    }

  @Test
  fun `Saturday, 3 h today and none on the days before, open the week, no weekend listener`() =
    runTest {
      val saturday = monday.plusDays(5)
      at(saturday, 20)
      sessions.value =
        listening(monday.minusWeeks(2), 8, 30.minutes) + listening(saturday, 12, 3.hours)

      assertEquals(
        expected = ListenerName.PartOfDay(DayPart.Afternoon, percent = 100),
        actual = state().name,
      )
    }

  @Test
  fun `Thursday, listening on Monday to Thursday, open the week, no creature of habit`() = runTest {
    at(monday.plusDays(3), 12)
    sessions.value = List(4) { listening(monday.plusDays(it.toLong()), 10, 30.minutes) }.flatten()

    assertEquals(
      expected = ListenerName.PartOfDay(DayPart.Morning, percent = 100),
      actual = state().name,
    )
  }

  @Test
  fun `Friday morning, listening on Monday to Thursday, none yet today, open the week, no creature of habit`() =
    runTest {
      at(today, 8)
      sessions.value = List(4) { listening(monday.plusDays(it.toLong()), 10, 30.minutes) }.flatten()

      assertEquals(
        expected = ListenerName.PartOfDay(DayPart.Morning, percent = 100),
        actual = state().name,
      )
    }

  @Test
  fun `Saturday morning, listening on Monday to Friday, none yet today, open the week, You are a creature of habit`() =
    runTest {
      at(monday.plusDays(5), 8)
      sessions.value = List(5) { listening(monday.plusDays(it.toLong()), 10, 30.minutes) }.flatten()

      assertEquals(expected = ListenerName.CreatureOfHabit, actual = state().name)
    }

  @Test
  fun `a full week with listening on each day, open the week, You are a creature of habit`() =
    runTest {
      at(sunday, 12)
      sessions.value = List(7) { listening(monday.plusDays(it.toLong()), 10, 30.minutes) }.flatten()

      assertEquals(expected = ListenerName.CreatureOfHabit, actual = state().name)
    }

  @Test
  fun `a week with 4 h, most of it between 12_00 and 17_00, and no other name true, open the week, You are an afternoon listener`() =
    runTest {
      sessions.value =
        listening(monday, 13, 90.minutes) +
          listening(monday.plusDays(1), 13, 90.minutes) +
          listening(monday.plusDays(2), 18, 1.hours)

      assertEquals(
        expected = ListenerName.PartOfDay(DayPart.Afternoon, percent = 75),
        actual = state().name,
      )
    }

  @Test
  fun `a week with 1 h 30 min, open the week, no name tile`() = runTest {
    sessions.value = listening(monday, 12, 90.minutes)

    assertNull(state().name)
  }

  @Test
  fun `a week where night owl and juggler are true, open the week today, and again tomorrow, night owl today, juggler tomorrow`() =
    runTest {
      at(monday.plusDays(3), 12)
      nightOwlAndJuggler()

      assertEquals(expected = ListenerName.NightOwl(percent = 100), actual = state().name)
      assertEquals(expected = ListenerName.Juggler(books = 3), actual = stateTomorrow().name)
    }

  @Test
  fun `a week where night owl and juggler are true, open the week two times on one day, the same name`() =
    runTest {
      nightOwlAndJuggler()

      assertEquals(expected = state().name, actual = state().name)
    }

  @Test
  fun `a week with 5 true facts, open the week today, and again tomorrow, one wide and two small facts today, other facts tomorrow`() =
    runTest {
      fiveFacts(monday)

      val days = listOf(state(), stateTomorrow(), stateTomorrow())

      days.forEach { assertEquals(expected = 2, actual = it.smallFacts.size) }
      assertNotEquals(illegal = days[0].wideFact, actual = days[1].wideFact)
      assertNotEquals(illegal = days[0].smallFacts, actual = days[1].smallFacts)
      assertEquals(expected = 2, actual = days.mapNotNull { it.wideFact }.toSet().size)
      assertEquals(expected = 3, actual = days.flatMap { it.smallFacts }.toSet().size)
    }

  @Test
  fun `a week with 3 true facts, open the week today, and again tomorrow, the same 3 facts`() =
    runTest {
      sessions.value =
        listOf(session(monday.atHour(12), 90.minutes)) +
          listening(monday.plusDays(2), 12, 45.minutes)

      val today = state()
      val tomorrow = stateTomorrow()

      assertIs<WideFact.WhenYouListen>(today.wideFact)
      assertEquals(
        expected =
          listOf(SmallFact.LongestDay(90.minutes, monday), SmallFact.LongestStretch(90.minutes)),
        actual = today.smallFacts,
      )
      assertEquals(expected = today.wideFact, actual = tomorrow.wideFact)
      assertEquals(expected = today.smallFacts, actual = tomorrow.smallFacts)
    }

  @Test
  fun `a week with 2 wide facts and 1 small fact, open the week today, and again tomorrow, the small fact stays and the wide tile alternates`() =
    runTest {
      val a = inLibrary(book(time = 0))
      val b = inLibrary(book(time = 0))
      sessions.value =
        listOf(session(monday.atHour(12), 90.minutes, bookId = a.id)) +
          listening(monday, 15, 45.minutes, b.id)

      val days = listOf(state(), stateTomorrow(), stateTomorrow())

      days.forEach {
        assertEquals(
          expected = listOf<SmallFact>(SmallFact.LongestStretch(90.minutes)),
          actual = it.smallFacts,
        )
      }
      assertEquals(
        expected = setOf(WideFact.WhenYouListen::class, WideFact.MostListened::class),
        actual = days.take(2).map { it.wideFact!!::class }.toSet(),
      )
      assertEquals(expected = days[0].wideFact, actual = days[2].wideFact)
    }

  @Test
  fun `a week with a name and 5 true facts, 3 weeks ago, open it today, and again tomorrow, it rotates like the current week`() =
    runTest {
      fiveFacts(monday.minusWeeks(3))
      repeat(3) { viewModel.onEarlierClick() }

      val today = state()
      val tomorrow = stateTomorrow()

      assertEquals(expected = monday.minusWeeks(3), actual = today.period.start)
      assertEquals(
        expected = ListenerName.PartOfDay(DayPart.Afternoon, percent = 100),
        actual = today.name,
      )
      assertEquals(expected = 2, actual = today.smallFacts.size)
      assertNotEquals(illegal = today.wideFact, actual = tomorrow.wideFact)
      assertNotEquals(illegal = today.smallFacts, actual = tomorrow.smallFacts)
    }

  @Test
  fun `Wednesday, 4 h this week, 2 h 40 min from Monday to Wednesday last week, the change shows, +1h 20m, more than last week`() =
    runTest {
      at(monday.plusDays(2), 12)
      sessions.value =
        listening(monday.minusDays(7), 8, 80.minutes) +
          listening(monday.minusDays(5), 8, 80.minutes) +
          listening(monday.minusDays(3), 8, 3.hours) +
          listening(monday, 8, 4.hours)

      assertEquals(
        expected = listOf(SmallFact.Change(by = 80.minutes)),
        actual = state().smallFacts,
      )
    }

  @Test
  fun `Wednesday, 2 h this week, 3 h from Monday to Wednesday last week, the change shows, -1h, less than last week`() =
    runTest {
      at(monday.plusDays(2), 12)
      sessions.value =
        listening(monday.minusDays(7), 8, 90.minutes) +
          listening(monday.minusDays(5), 8, 90.minutes) +
          listening(monday, 8, 2.hours)

      assertEquals(expected = listOf(SmallFact.Change(by = -1.hours)), actual = state().smallFacts)
    }

  @Test
  fun `the 20th of November, 3 h this month, 1 h up to the 20th of October, the change shows, +2h, more than last month`() =
    runTest {
      sessions.value =
        listening(LocalDate.of(2026, 10, 1), 8, 1.hours) +
          listening(LocalDate.of(2026, 10, 25), 8, 2.hours) +
          listening(LocalDate.of(2026, 11, 5), 8, 3.hours)
      viewModel.onKindClick(PeriodKind.Month)

      assertEquals(expected = listOf(SmallFact.Change(by = 2.hours)), actual = state().smallFacts)
    }

  @Test
  fun `the 20th of November, 3 h this year, 1 h up to the 20th of November last year, the change shows, +2h, more than last year`() =
    runTest {
      sessions.value =
        listening(LocalDate.of(2025, 1, 1), 8, 1.hours) +
          listening(LocalDate.of(2025, 12, 10), 8, 2.hours) +
          listening(LocalDate.of(2026, 3, 2), 8, 3.hours)
      viewModel.onKindClick(PeriodKind.Year)

      assertEquals(expected = listOf(SmallFact.Change(by = 2.hours)), actual = state().smallFacts)
    }

  @Test
  fun `a past February with 3 h, and 1 h 30 min in January, open it, the change counts all of January, +1h 30m`() =
    runTest {
      at(LocalDate.of(2026, 3, 10), 12)
      sessions.value =
        listening(LocalDate.of(2026, 1, 1), 8, 30.minutes) +
          listening(LocalDate.of(2026, 1, 30), 8, 1.hours) +
          listening(LocalDate.of(2026, 2, 10), 8, 3.hours)
      viewModel.onKindClick(PeriodKind.Month)
      viewModel.onEarlierClick()

      assertEquals(
        expected = listOf(SmallFact.Change(by = 90.minutes)),
        actual = state().smallFacts,
      )
    }

  @Test
  fun `a year with 1 h in March and 2 h in November, open the year, the longest month shows, 2h, in November`() =
    runTest {
      sessions.value =
        listening(LocalDate.of(2026, 3, 2), 8, 1.hours) + listening(today, 8, 2.hours)
      viewModel.onKindClick(PeriodKind.Year)

      assertEquals(
        expected = listOf<SmallFact>(SmallFact.LongestMonth(2.hours, Month.NOVEMBER)),
        actual = state().smallFacts,
      )
    }

  @Test
  fun `a week with less than a minute on each of 2 days, open the week, no longest day`() =
    runTest {
      sessions.value =
        listOf(
          session(monday.atHour(8), 40.seconds),
          session(monday.plusDays(2).atHour(8), 50.seconds),
        )

      assertEquals(expected = emptyList(), actual = state().smallFacts)
    }

  @Test
  fun `the first week that Voice counted, open the week, no change`() = runTest {
    sessions.value = listening(monday, 8, 2.hours)

    assertEquals(expected = emptyList(), actual = state().smallFacts)
  }

  @Test
  fun `the week after the first one, and Voice counted from the Wednesday of the first one, open the week, no change`() =
    runTest {
      sessions.value = listening(monday.minusDays(5), 8, 1.hours) + listening(monday, 8, 2.hours)

      assertEquals(expected = emptyList(), actual = state().smallFacts)
    }

  @Test
  fun `all time, pick the chip, no change, no longest day`() = runTest {
    sessions.value =
      listening(monday.minusDays(7), 8, 1.hours) +
        listening(monday.minusDays(5), 8, 1.hours) +
        listening(monday, 8, 1.hours) +
        listening(monday.plusDays(2), 8, 30.minutes)

    assertEquals(
      expected = listOf(SmallFact.Change(by = -30.minutes), SmallFact.LongestDay(1.hours, monday)),
      actual = state().smallFacts,
    )

    viewModel.onKindClick(PeriodKind.AllTime)

    assertEquals(expected = emptyList(), actual = state().smallFacts)
  }

  @Test
  fun `2 h listened at 1,1x, open the week, no saved fact`() = runTest {
    sessions.value = listening(monday, 8, 2.hours, speed = 1.1)

    assertEquals(expected = emptyList(), actual = state().smallFacts)
  }

  @Test
  fun `2 h listened at 1,5x, the saved fact shows, 1h, saved at 1,5x`() = runTest {
    sessions.value = listening(monday, 8, 2.hours, speed = 1.5)

    assertEquals(
      expected = listOf(SmallFact.Saved(saved = 1.hours, speed = 1.5F)),
      actual = state().smallFacts,
    )
  }

  @Test
  fun `a week with one book, open the week, no Most listened`() = runTest {
    val book = inLibrary(book(time = 0))
    sessions.value = listening(monday, 8, 150.minutes, book.id)

    val today = state()
    val tomorrow = stateTomorrow()

    assertIs<WideFact.WhenYouListen>(today.wideFact)
    assertIs<WideFact.WhenYouListen>(tomorrow.wideFact)
  }

  @Test
  fun `a week with 3 h of book A and 1 h of book B, Most listened shows, book A with 3h`() =
    runTest {
      val a = inLibrary(book(time = 0))
      val b = inLibrary(book(time = 0))
      sessions.value = listening(monday, 8, 3.hours, a.id) + listening(monday, 14, 1.hours, b.id)

      // it shares the wide tile with "When you listen", so it shows on one of two days
      assertContains(
        listOf(state().wideFact, stateTomorrow().wideFact),
        WideFact.MostListened(a.toStatsBook(), listened = 3.hours),
      )
    }

  @Test
  fun `Most listened, tap it, the book opens`() = runTest {
    val a = inLibrary(book(time = 0))
    val b = inLibrary(book(time = 0))
    sessions.value = listening(monday, 8, 1.hours, a.id) + listening(monday, 14, 30.minutes, b.id)

    val fact = assertIs<WideFact.MostListened>(state().wideFact)
    assertTrue(fact.book.inLibrary)
    viewModel.onBookClick(fact.book.id)

    verify(exactly = 1) { navigator.goTo(Destination.Playback(a.id)) }
  }

  @Test
  fun `the most listened book, later removed with its files, Most listened shows, its name without a cover, and no tap`() =
    runTest {
      val removed = book(name = "Dune")
      contents.value = listOf(removed.content.copy(isActive = false))
      sessions.value = listening(monday, 8, 1.hours, removed.id) + listening(monday, 14, 30.minutes)

      assertEquals(
        expected =
          WideFact.MostListened(
            StatsBookViewState(removed.id, "Dune", cover = null, inLibrary = false),
            listened = 1.hours,
          ),
        actual = state().wideFact,
      )
    }

  @Test
  fun `this week, a book with 9 h left at 1x, 2 h per day in the last 14 days, the forecast shows, 9h left, and the day 4 days from today`() =
    runTest {
      val book = inLibrary(bookWith9HoursLeft())
      twoHoursPerDay(book.id)

      assertEquals(
        expected =
          WideFact.Forecast(
            book.toStatsBook(),
            left = 9.hours,
            day = today.plusDays(4),
            today = today,
          ),
        actual = state().wideFact,
      )
    }

  @Test
  fun `this week, a book with 9 h left at 1,5x, the forecast shows, 6h left`() = runTest {
    val book = inLibrary(bookWith9HoursLeft().update { it.copy(playbackSpeed = 1.5F) })
    twoHoursPerDay(book.id)

    assertEquals(
      expected =
        WideFact.Forecast(
          book.toStatsBook(),
          left = 6.hours,
          day = today.plusDays(2),
          today = today,
        ),
      actual = state().wideFact,
    )
  }

  @Test
  fun `this week, a book that needs 80 days at the pace, open the week, no forecast`() = runTest {
    val book = inLibrary(bookWith9HoursLeft())
    // 1 h 35 min in 14 days: 9 h need 80 days
    sessions.value =
      listening(today.minusDays(14), 8, 30.minutes, book.id) +
        listening(monday, 8, 95.minutes, book.id)

    assertNull(state().wideFact)
  }

  @Test
  fun `this week, a book with its last day 60 days away, open the week, the forecast shows`() =
    runTest {
      val book = inLibrary(bookWith9HoursLeft())
      sessions.value =
        listening(today.minusDays(14), 8, 30.minutes, book.id) +
          listening(monday.minusDays(6), 8, 1.hours, book.id) +
          listening(monday, 8, 64.minutes, book.id)

      val fact = assertIs<WideFact.Forecast>(state().wideFact)
      assertEquals(expected = today.plusDays(60), actual = fact.day)
    }

  @Test
  fun `this week, no listening in the last 14 days, open the week, no forecast`() = runTest {
    val book = inLibrary(bookWith9HoursLeft())
    sessions.value = listening(today.minusDays(14), 8, 2.hours, book.id)

    assertNull(state().wideFact)
    assertNull(forecast(sessions.value, listOf(book), today.minusDays(14), today))
  }

  @Test
  fun `the 25th, a month with listening on the 1st to the 5th, open the month, no forecast`() =
    runTest {
      at(LocalDate.of(2026, 11, 25), 12)
      val book = inLibrary(bookWith9HoursLeft())
      sessions.value =
        List(5) { listening(LocalDate.of(2026, 11, 1 + it), 8, 20.minutes, book.id) }.flatten()
      viewModel.onKindClick(PeriodKind.Month)

      assertNull(state().wideFact)
    }

  @Test
  fun `book B finished yesterday, book A at 10 percent last played a year ago, open the week, no forecast`() =
    runTest {
      val a = inLibrary(bookWith9HoursLeft())
      val b =
        inLibrary(
          book(
            chapters = listOf(chapter(duration = 10.hours.inWholeMilliseconds)),
            time = 10.hours.inWholeMilliseconds,
          ),
        )
      sessions.value =
        listening(today.minusYears(1), 8, 1.hours, a.id) +
          listening(monday.minusDays(3), 8, 2.hours, b.id) +
          listening(today.minusDays(1), 8, 30.minutes, b.id)

      assertNull(state().wideFact)
    }

  @Test
  fun `this week, the book played last is hidden, the forecast shows, for the book before it`() =
    runTest {
      val shown = inLibrary(bookWith9HoursLeft())
      val hidden = inLibrary(bookWith9HoursLeft(hidden = true))
      sessions.value =
        listening(monday.minusDays(3), 8, 2.hours, shown.id) +
          listening(monday.minusDays(2), 8, 2.hours, hidden.id) +
          listening(monday, 8, 30.minutes, hidden.id)

      val fact = assertIs<WideFact.Forecast>(state().wideFact)
      assertEquals(expected = shown.id, actual = fact.book.id)
    }

  @Test
  fun `this week, two books in progress, the forecast shows, for the book played last`() = runTest {
    val a = inLibrary(bookWith9HoursLeft())
    val b = inLibrary(bookWith9HoursLeft())
    sessions.value =
      listening(monday.minusDays(3), 8, 1.hours, a.id) +
        listening(monday.minusDays(2), 8, 1.hours, b.id) +
        listening(monday.minusDays(1), 8, 2.hours, a.id) +
        listening(monday, 8, 30.minutes, b.id)

    val fact = assertIs<WideFact.Forecast>(state().wideFact)
    assertEquals(expected = b.id, actual = fact.book.id)
  }

  @Test
  fun `all time, a book in progress, pick the chip, no forecast`() = runTest {
    val book = inLibrary(bookWith9HoursLeft())
    twoHoursPerDay(book.id)

    assertIs<WideFact.Forecast>(state().wideFact)

    viewModel.onKindClick(PeriodKind.AllTime)

    assertIs<WideFact.WhenYouListen>(state().wideFact)
    assertIs<WideFact.WhenYouListen>(stateTomorrow().wideFact)
  }

  @Test
  fun `last week, open it, no forecast`() = runTest {
    val book = inLibrary(bookWith9HoursLeft())
    sessions.value =
      List(3) { listening(monday.minusDays(7L - it), 8, 330.minutes, book.id) }.flatten() +
        listening(monday, 8, 30.minutes, book.id)

    assertIs<WideFact.Forecast>(state().wideFact)

    viewModel.onEarlierClick()

    assertIs<WideFact.WhenYouListen>(state().wideFact)
  }

  @Test
  fun `a stretch from 23_30 to 0_30, When you listen shows, half of it counts at 23_00 and half at 0_00, and all of it on the first day`() =
    runTest {
      // 23:30 in Berlin in winter is 22:30 in UTC
      sessions.value =
        listening(monday, 11, 90.minutes) +
          session(Instant.parse("2026-11-17T22:30:00Z"), 1.hours, utcOffset = 1.hours)

      val state = state()

      val hours = assertIs<WideFact.WhenYouListen>(state.wideFact).hours
      assertEquals(expected = 30.minutes, actual = hours[23])
      assertEquals(expected = 30.minutes, actual = hours[0])
      assertEquals(expected = 1.hours, actual = state.bars[1])
      assertEquals(expected = Duration.ZERO, actual = state.bars[2])
    }

  @Test
  fun `a week with 3 h in the evening and 1 h in the morning, When you listen shows, evening 75 percent, morning 25 percent, and the evening stands out`() =
    runTest {
      sessions.value = listening(monday, 18, 3.hours) + listening(monday.plusDays(1), 8, 1.hours)

      val fact = assertIs<WideFact.WhenYouListen>(state().wideFact)
      assertEquals(expected = 75, actual = fact.percent(DayPart.Evening))
      assertEquals(expected = 25, actual = fact.percent(DayPart.Morning))
      assertEquals(expected = DayPart.Evening, actual = fact.top)
    }

  @Test
  fun `a month with 4 weeks of listening, most of it on Sundays, Your day shows, Sunday, is your day`() =
    runTest {
      fourWeeksWithSundays()
      viewModel.onKindClick(PeriodKind.Month)

      assertContains(state().smallFacts, SmallFact.YourDay(DayOfWeek.SUNDAY))
    }

  @Test
  fun `a month with 3 weeks of listening, open the month, no Your day`() = runTest {
    val first = LocalDate.of(2026, 11, 1)
    sessions.value = listOf(1, 8, 15).flatMap { listening(first.withDayOfMonth(it), 8, 30.minutes) }
    viewModel.onKindClick(PeriodKind.Month)

    assertEquals(
      expected = listOf<SmallFact>(SmallFact.LongestDay(30.minutes, first)),
      actual = state().smallFacts,
    )
  }

  @Test
  fun `a year with 4 weeks of listening, most of it on Sundays, Your day shows, Sunday, is your day`() =
    runTest {
      fourWeeksWithSundays()
      viewModel.onKindClick(PeriodKind.Year)

      assertEquals(
        expected = listOf<SmallFact>(SmallFact.YourDay(DayOfWeek.SUNDAY)),
        actual = state().smallFacts,
      )
    }

  @Test
  fun `a week, open it, no Your day`() = runTest {
    fourWeeksWithSundays()

    assertEquals(expected = listOf(SmallFact.Change(by = 20.minutes)), actual = state().smallFacts)
  }

  @Test
  fun `a week with a stretch of 2 h 15 min, the longest stretch shows, 2h 15m, in one go`() =
    runTest {
      sessions.value = listOf(session(monday.atHour(8), 135.minutes))

      assertEquals(
        expected = listOf(SmallFact.LongestStretch(listened = 135.minutes)),
        actual = state().smallFacts,
      )
    }

  @Test
  fun `Thursday, listening on each day from Monday, and no longer run before, the streak shows, 4 days, in a row, your longest streak`() =
    runTest {
      at(monday.plusDays(3), 12)
      sessions.value = daysInARow(monday, days = 4)

      assertContains(state().smallFacts, SmallFact.Streak(current = 4, longest = 4))
    }

  @Test
  fun `Thursday, listening on each day from Monday, and a run of 9 days in October, the streak shows, 4 days, in a row, the longest 9 days`() =
    runTest {
      at(monday.plusDays(3), 12)
      sessions.value =
        daysInARow(LocalDate.of(2026, 10, 1), days = 9) +
          daysInARow(monday.minusWeeks(1), days = 4) +
          daysInARow(monday, days = 4)

      assertContains(state().smallFacts, SmallFact.Streak(current = 4, longest = 9))
    }

  @Test
  fun `Thursday morning, listening on Monday to Wednesday, none yet today, the streak shows, 3 days`() =
    runTest {
      at(monday.plusDays(3), 7)
      sessions.value = daysInARow(monday, days = 3)

      assertContains(state().smallFacts, SmallFact.Streak(current = 3, longest = 3))
    }

  @Test
  fun `Friday, listening on Monday to Wednesday, none on Thursday and today, the streak shows, the longest one, 9 days, is your longest streak`() =
    runTest {
      sessions.value =
        daysInARow(LocalDate.of(2026, 10, 1), days = 9) +
          daysInARow(monday.minusWeeks(1), days = 3) +
          daysInARow(monday, days = 3)

      assertContains(state().smallFacts, SmallFact.Streak(current = 0, longest = 9))
    }

  @Test
  fun `all time, Thursday, listening on each day from Monday, pick the chip, the streak shows`() =
    runTest {
      at(monday.plusDays(3), 12)
      sessions.value = daysInARow(monday, days = 4)
      viewModel.onKindClick(PeriodKind.AllTime)

      assertEquals(
        expected = listOf<SmallFact>(SmallFact.Streak(current = 4, longest = 4)),
        actual = state().smallFacts,
      )
    }

  @Test
  fun `never 2 days in a row, open the week, no streak`() = runTest {
    sessions.value = daysInARow(monday, days = 1) + daysInARow(monday.plusDays(2), days = 1)

    assertEquals(
      expected = listOf<SmallFact>(SmallFact.LongestDay(30.minutes, monday)),
      actual = state().smallFacts,
    )
  }

  @Test
  fun `last week, open it, no streak`() = runTest {
    sessions.value = daysInARow(monday.minusWeeks(1), days = 4) + daysInARow(monday, days = 1)

    assertContains(state().smallFacts, SmallFact.Streak(current = 0, longest = 4))

    viewModel.onEarlierClick()

    assertEquals(
      expected = listOf<SmallFact>(SmallFact.LongestDay(30.minutes, monday.minusWeeks(1))),
      actual = state().smallFacts,
    )
  }

  @Test
  fun `a streak of 4 days ends, open the app the day after, no message and no notification`() =
    runTest {
      at(monday.plusDays(5), 12)
      sessions.value = daysInARow(monday, days = 4)

      assertContains(state().smallFacts, SmallFact.Streak(current = 0, longest = 4))
    }

  @Test
  fun `a week with 3 finished books, open the week, a shelf with 3 covers and the number 3`() =
    runTest {
      val finished = List(3) { inLibrary(book(time = 0)) }
      sessions.value = finished.mapIndexed { index, book ->
        session(
          monday.plusDays(index.toLong()).atHour(8),
          30.minutes,
          bookId = book.id,
          reachedEnd = true,
        )
      }

      assertEquals(
        expected = finished.reversed().map { it.toStatsBook() },
        actual = state().finished.map { it.book },
      )
    }

  @Test
  fun `a week without listening, open it, nothing here yet, with no name and no facts`() = runTest {
    at(monday.plusWeeks(1), 8)
    val book = inLibrary(bookWith9HoursLeft())
    sessions.value =
      List(7) { listening(monday.plusDays(it.toLong()), 8, 3.hours, book.id) }.flatten()

    val state = state()

    assertTrue(state.empty)
    assertNull(state.name)
    assertNull(state.wideFact)
    assertEquals(expected = emptyList(), actual = state.smallFacts)
  }

  @Test
  fun `the screen, with analytics on, open it, the screen and the kind of period are reported, no name and no fact is reported`() =
    runTest {
      val _ = inLibrary(bookWith9HoursLeft())
      fiveFacts(monday)

      val state = state()

      assertTrue(state.name != null && state.wideFact != null && state.smallFacts.isNotEmpty())
      verify(exactly = 1) {
        analytics.event("listening_stats_period", mapOf("period" to "week"))
      }
      verify(exactly = 1) { analytics.event(any(), any()) }
    }

  private fun nightOwlAndJuggler() {
    sessions.value =
      List(3) { listening(monday.plusDays(it.toLong()), 22, 1.hours, BookId("book$it")) }.flatten()
  }

  private fun fiveFacts(from: LocalDate) {
    val a = inLibrary(book(time = 0))
    val b = inLibrary(book(time = 0))
    sessions.value =
      listOf(session(from.atHour(12), 90.minutes, audio = 135.minutes, bookId = a.id)) +
        listening(from.plusDays(2), 12, 45.minutes, b.id)
  }

  private fun bookWith9HoursLeft(hidden: Boolean = false): Book =
    book(
      chapters = listOf(chapter(duration = 10.hours.inWholeMilliseconds)),
      time = 1.hours.inWholeMilliseconds,
      hidden = hidden,
    )

  // 28 h in the 14 days up to today, and too little this week for another wide fact
  private fun twoHoursPerDay(bookId: BookId) {
    sessions.value =
      List(5) { listening(today.minusDays(13L - it), 8, 330.minutes, bookId) }.flatten() +
        listening(monday, 8, 30.minutes, bookId)
  }

  private fun fourWeeksWithSundays() {
    sessions.value =
      listOf(1, 8, 15).flatMap { listening(LocalDate.of(2026, 11, it), 8, 30.minutes) } +
        listening(monday.plusDays(1), 8, 20.minutes)
  }

  private fun daysInARow(
    from: LocalDate,
    days: Int,
  ): List<ListeningSession> =
    List(days) { listening(from.plusDays(it.toLong()), 6, 30.minutes) }.flatten()

  private fun inLibrary(book: Book): Book {
    books.value += book
    contents.value += book.content
    return book
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

  private suspend fun TestScope.stateTomorrow(): ListeningStatsViewState {
    now += 1.days
    return state()
  }

  private fun at(
    day: LocalDate,
    hour: Int,
  ) {
    now = day.atHour(hour)
  }

  // in pieces of 30 minutes at most, so that no stretch is long enough for a name or a fact
  private fun listening(
    day: LocalDate,
    hour: Int,
    listened: Duration,
    bookId: BookId = BookId("book"),
    speed: Double = 1.0,
  ): List<ListeningSession> {
    val pieces = ceil(listened / 30.minutes).toInt()
    val piece = listened / pieces
    return List(pieces) {
      session(day.atHour(hour) + piece * it, piece, audio = piece * speed, bookId = bookId)
    }
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

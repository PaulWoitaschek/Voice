package voice.features.listeningStats

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningSessionRepo
import voice.navigation.Navigator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

// The ViewModel assembles Room aggregation and period selection into page state; these tests verify that assembly.
class ListeningStatsViewModelTest {

  // A fixed clock makes period ranges predictable so tests never depend on the real calendar.
  private val clock = Clock.fixed(Instant.parse("2024-01-15T12:00:00Z"), ZoneId.of("UTC"))
  private val bookId = BookId("book")

  // With listening time recorded, the state shows the book title and aggregated duration, not empty.
  @Test
  fun `view state shows book title and duration`() = runTest {
    val viewModel = viewModel(durationMs = 90_000)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      // collectAsState emits a default frame first, then the real state from the Room flow.
      skipItems(1)
      val state = awaitItem()
      assertEquals(expected = "My Book", actual = state.title)
      assertEquals(expected = 90_000, actual = state.durationMs)
      assertEquals(expected = false, actual = state.empty)
      assertEquals(expected = ListeningStatsPeriod.Day, actual = state.selectedPeriod)
      cancelAndIgnoreRemainingEvents()
    }
  }

  // Zero duration means nothing has been recorded yet, so the page renders the empty hint.
  @Test
  fun `zero duration renders empty state`() = runTest {
    val viewModel = viewModel(durationMs = 0)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      // The second frame is the stable state after the book name loads, but it must stay empty.
      skipItems(1)
      assertEquals(expected = true, actual = awaitItem().empty)
      cancelAndIgnoreRemainingEvents()
    }
  }

  // Before book data loads, the title falls back to the book id instead of going blank.
  @Test
  fun `missing book falls back to book id as title`() = runTest {
    val viewModel = viewModel(durationMs = 0, book = null)

    moleculeFlow(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = bookId.value, actual = awaitItem().title)
      cancelAndIgnoreRemainingEvents()
    }
  }

  // Switching periods must re-query the aggregated duration with the new week range.
  @Test
  fun `switching period re-queries duration with new range`() = runTest {
    val originalLocale = Locale.getDefault()
    Locale.setDefault(Locale.GERMANY)
    try {
      val repo = RecordingListeningSessionRepo()
      val viewModel = viewModel(listeningSessionRepo = repo)

      moleculeFlow(RecompositionMode.Immediate) {
        viewModel.viewState()
      }.test {
        // Wait for the default and data frames so the period is not switched before the initial flow is collected.
        skipItems(1)
        assertEquals(expected = ListeningStatsPeriod.Day, actual = awaitItem().selectedPeriod)
        viewModel.selectPeriod(ListeningStatsPeriod.Week)
        val state = awaitItem()
        assertEquals(expected = ListeningStatsPeriod.Week, actual = state.selectedPeriod)
        cancelAndIgnoreRemainingEvents()
      }
      // German locale starts weeks on Monday; Jan 15 2024 is a Monday, so the week runs Jan 15 to Jan 22.
      val lastCall = repo.calls.last()
      assertEquals(expected = bookId, actual = lastCall.first)
      assertEquals(expected = Instant.parse("2024-01-15T00:00:00Z"), actual = lastCall.second)
      assertEquals(expected = Instant.parse("2024-01-22T00:00:00Z"), actual = lastCall.third)
    } finally {
      Locale.setDefault(originalLocale)
    }
  }

  // Closing should delegate to the navigator to go back.
  @Test
  fun `close navigates back`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(navigator = navigator)

    viewModel.close()

    verify { navigator.goBack() }
  }

  private fun viewModel(
    durationMs: Long = 10_000,
    book: Book? = book(),
    navigator: Navigator = mockk(relaxed = true),
    listeningSessionRepo: ListeningSessionRepo = mockk(relaxed = true) {
      every { durationFlow(any(), any(), any()) } returns MutableStateFlow(durationMs)
    },
  ): ListeningStatsViewModel {
    val bookRepository = mockk<BookRepository>(relaxed = true) {
      every { flow(bookId) } returns MutableStateFlow(book)
    }
    return ListeningStatsViewModel(
      bookRepository = bookRepository,
      listeningSessionRepo = listeningSessionRepo,
      navigator = navigator,
      clock = clock,
      bookId = bookId,
    )
  }

  // The test book only needs its name; the rest is left to mockk defaults.
  private fun book(): Book {
    return mockk {
      every { content } returns mockk {
        every { name } returns "My Book"
      }
    }
  }

  // Records every period-query parameter to verify the range follows period switches.
  private class RecordingListeningSessionRepo : ListeningSessionRepo {

    val calls = mutableListOf<Triple<BookId, Instant, Instant>>()

    // Every new query range is recorded so assertions can check it.
    override fun durationFlow(
      bookId: BookId,
      startedAt: Instant,
      endedAt: Instant,
    ): Flow<Long> {
      calls += Triple(bookId, startedAt, endedAt)
      return MutableStateFlow(10_000L)
    }

    // Write and cleanup paths are not covered by this test.
    override suspend fun put(session: voice.core.data.ListeningSession) = Unit

    // Per-book cleanup is not covered by this test.
    override suspend fun deleteForBook(bookId: BookId) = Unit
  }
}

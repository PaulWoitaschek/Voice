package voice.features.bookOverview.series

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.data.CustomMetadata
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.book
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.editBookCategory.BookCategoryChanger
import voice.features.bookOverview.hideBook.BookHider
import voice.features.bookOverview.hideBook.HiddenBooks
import voice.features.bookOverview.select.BookSelectionRequests
import voice.navigation.Destination
import voice.navigation.Navigator

class SeriesActionsViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val dispatcherProvider =
    DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
  private val navigator = mockk<Navigator>(relaxUnitFun = true)
  private val first = book(series = "Saga", part = "1")
  private val second = book(series = "saga", part = "2")
  private val hidden = book(series = "Saga", part = "3", hidden = true)
  private val other = book(series = "Other")
  private val books = MutableStateFlow(listOf(second, other, hidden, first))
  private val repo =
    mockk<BookRepository> {
      every { flow() } returns books
      coEvery { all() } answers { books.value }
      coEvery { updateCustomMetadata(any(), any()) } just runs
    }
  private val bookHider = BookHider(repo)
  private val bookCategoryChanger = mockk<BookCategoryChanger>(relaxUnitFun = true)
  private val bookSelectionRequests = BookSelectionRequests()
  private val viewModel =
    SeriesActionsViewModel(
      repo,
      bookHider,
      bookCategoryChanger,
      navigator,
      bookSelectionRequests,
      dispatcherProvider,
      "Saga",
    )

  @Test
  fun `the sheet shows the series`() = runTest {
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = null, actual = awaitItem())
        val state = awaitItem()!!.series
        assertEquals(expected = "Saga", actual = state.name)
        assertEquals(expected = 3, actual = state.bookCount)
        assertEquals(expected = false, actual = state.hidden)

        books.value = listOf(first.hide(), second.hide(), hidden, other)

        assertEquals(expected = true, actual = awaitItem()!!.series.hidden)
      }
  }

  @Test
  fun `hiding a series hides its books and the undo leaves the ones hidden before`() = runTest {
    bookHider.hidden.test {
      viewModel.onItemClick(BottomSheetItem.Hide)

      assertEquals(
        expected = HiddenBooks(listOf(first.id, second.id), "Saga", series = true),
        actual = awaitItem(),
      )
    }
    assertEquals(expected = true, actual = update(first.id)(first.custom).hidden)
    assertEquals(expected = true, actual = update(second.id)(second.custom).hidden)
    coVerify(exactly = 2) { repo.updateCustomMetadata(any(), any()) }
  }

  @Test
  fun `showing a series shows all of its books`() = runTest {
    books.value = listOf(first.hide(), second.hide(), hidden, other)

    viewModel.onItemClick(BottomSheetItem.Show)

    listOf(first, second, hidden).forEach { book ->
      assertEquals(
        expected = false,
        actual = update(book.id)(book.custom.copy(hidden = true)).hidden,
      )
    }
    coVerify(exactly = 3) { repo.updateCustomMetadata(any(), any()) }
  }

  @Test
  fun `the sheet closes once the series has no books`() = runTest {
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = null, actual = awaitItem())
        assertEquals(expected = 3, actual = awaitItem()!!.series.bookCount)

        books.value = listOf(other)

        assertEquals(expected = null, actual = awaitItem())
      }
    verify { navigator.remove(Destination.SeriesActions("Saga")) }
    coVerify(exactly = 0) { repo.updateCustomMetadata(any(), any()) }
  }

  @Test
  fun `the sheet offers the statuses the series isn't in and counts the books that lose progress`() =
    runTest {
      books.value =
        listOf(
          book(series = "Saga", time = 0),
          book(series = "Saga", time = 1_000),
          book(series = "Saga", time = 20_000),
        )

      statuses().test {
        assertEquals(expected = null, actual = awaitItem())
        assertEquals(
          expected =
            listOf(
              SeriesActionsViewState.Status(
                BottomSheetItem.BookCategoryMarkAsNotStarted,
                resets = 2,
              ),
              SeriesActionsViewState.Status(
                BottomSheetItem.BookCategoryMarkAsCompleted,
                resets = 1,
              ),
            ),
          actual = awaitItem(),
        )
      }
    }

  @Test
  fun `a series not started yet only offers to complete it, and it loses nothing`() = runTest {
    books.value = listOf(book(series = "Saga", time = 0), book(series = "Saga", time = 0))

    statuses().test {
      assertEquals(expected = null, actual = awaitItem())
      assertEquals(
        expected =
          listOf(
            SeriesActionsViewState.Status(BottomSheetItem.BookCategoryMarkAsCompleted, resets = 0),
          ),
        actual = awaitItem(),
      )
    }
  }

  @Test
  fun `completing a series completes the books that aren't yet`() = runTest {
    val notStarted = book(series = "Saga", time = 0)
    val current = book(series = "Saga", time = 1_000)
    val finished = book(series = "Saga", time = 20_000)
    books.value = listOf(notStarted, current, finished, other)

    viewModel.onItemClick(BottomSheetItem.BookCategoryMarkAsCompleted)

    coVerify { bookCategoryChanger.change(notStarted.id, BookCategory.FINISHED) }
    coVerify { bookCategoryChanger.change(current.id, BookCategory.FINISHED) }
    coVerify(exactly = 2) { bookCategoryChanger.change(any(), any()) }
  }

  @Test
  fun `marking a series as not started resets its hidden books too`() = runTest {
    viewModel.onItemClick(BottomSheetItem.BookCategoryMarkAsNotStarted)

    listOf(first, second, hidden).forEach { book ->
      coVerify { bookCategoryChanger.change(book.id, BookCategory.NOT_STARTED) }
    }
    coVerify(exactly = 3) { bookCategoryChanger.change(any(), any()) }
  }

  @Test
  fun `select only shows while a screen under the sheet can select`() = runTest {
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .map { it?.canSelect }
      .test {
        assertEquals(expected = null, actual = awaitItem())
        assertEquals(expected = false, actual = awaitItem())

        backgroundScope.launch { bookSelectionRequests.requests.collect {} }

        assertEquals(expected = true, actual = awaitItem())
      }
  }

  @Test
  fun `select asks the screen to select the books of the series`() = runTest {
    val requests = mutableListOf<List<BookId>>()
    backgroundScope.launch { bookSelectionRequests.requests.collect { requests += it } }
    runCurrent()

    viewModel.onItemClick(BottomSheetItem.Select)
    runCurrent()

    assertEquals(
      expected = setOf(first.id, second.id, hidden.id),
      actual = requests.single().toSet(),
    )
  }

  private fun TestScope.statuses(): Flow<List<SeriesActionsViewState.Status>?> =
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .map { it?.statuses }

  private fun update(id: BookId): (CustomMetadata) -> CustomMetadata {
    val update = slot<(CustomMetadata) -> CustomMetadata>()
    coVerify { repo.updateCustomMetadata(id, capture(update)) }
    return update.captured
  }

  private fun Book.hide(): Book = copy(custom = custom.copy(hidden = true))
}

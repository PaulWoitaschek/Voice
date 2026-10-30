package voice.features.bookOverview.series

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.PlayerController
import voice.features.bookOverview.MemoryDataStore
import voice.features.bookOverview.book
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SeriesViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
  private val navigator = mockk<Navigator>(relaxed = true)
  private val playerController = mockk<PlayerController>(relaxed = true)
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val books = MutableStateFlow<List<Book>>(emptyList())

  @Test
  fun `the books are in order and the header names their author once`() = runTest {
    val second = book(name = "Second", series = "Saga", part = "2", author = "Author", time = 0)
    val first = book(name = "First", series = "saga", part = "1", author = "Author", time = 20_000)
    val other = book(name = "Other", series = "Other", author = "Author")
    books.value = listOf(second, other, first)

    testState {
      val state = awaitItem()!!
      assertEquals(expected = "Saga", actual = state.name)
      assertEquals(expected = "Author", actual = state.author)
      assertEquals(expected = 2, actual = state.bookCount)
      assertEquals(expected = 1, actual = state.finishedCount)
      assertEquals(
        expected = listOf(
          SeriesViewState.Book(book = first.toItemViewStateWithoutAuthor(), part = "1", finished = true),
          SeriesViewState.Book(book = second.toItemViewStateWithoutAuthor(), part = "2", finished = false),
        ),
        actual = state.books,
      )
    }
  }

  @Test
  fun `the button continues the book in progress played last`() = runTest {
    val first = book(series = "Saga", part = "1", time = 1_000, lastPlayedAt = Instant.ofEpochSecond(2))
    val second = book(series = "Saga", part = "2", time = 1_000, lastPlayedAt = Instant.ofEpochSecond(1))
    books.value = listOf(first, second)

    testState {
      assertEquals(
        expected = SeriesViewState.Action(bookId = first.id, part = "1", continues = true),
        actual = awaitItem()!!.action,
      )
    }
  }

  @Test
  fun `without a book in progress the button starts the first book not finished`() = runTest {
    val first = book(series = "Saga", part = "1", time = 20_000)
    val second = book(series = "Saga", time = 0)
    books.value = listOf(first, second)

    testState {
      assertEquals(
        expected = SeriesViewState.Action(bookId = second.id, part = null, continues = false),
        actual = awaitItem()!!.action,
      )
    }
  }

  @Test
  fun `a finished series has no button`() = runTest {
    books.value = listOf(book(series = "Saga", time = 20_000), book(series = "Saga", time = 20_000))

    testState {
      assertEquals(expected = null, actual = awaitItem()!!.action)
    }
  }

  @Test
  fun `the button plays the book and opens the player`() = runTest {
    val book = book(series = "Saga", time = 1_000)
    currentBookStore.updateData { BookId("other") }

    viewModel().onActionClick(book.id)

    assertEquals(expected = book.id, actual = currentBookStore.data.first())
    verifyOrder {
      playerController.pauseIfCurrentBookDifferentFrom(book.id)
      playerController.play(any())
      navigator.goTo(Destination.Playback(book.id))
    }
  }

  @Test
  fun `the button keeps playing the current book`() = runTest {
    val book = book(series = "Saga", time = 1_000)
    currentBookStore.updateData { book.id }

    viewModel().onActionClick(book.id)

    verify(exactly = 0) { playerController.pauseIfCurrentBookDifferentFrom(any()) }
    verifyOrder {
      playerController.play(any())
      navigator.goTo(Destination.Playback(book.id))
    }
  }

  @Test
  fun `the screen closes once the series has no books`() = runTest {
    books.value = listOf(book(series = "Saga"))

    testState {
      assertEquals(expected = 1, actual = awaitItem()!!.bookCount)
      books.value = emptyList()
      assertEquals(expected = null, actual = awaitItem())
    }
    verify { navigator.remove(Destination.Series("Saga")) }
  }

  private fun Book.toItemViewStateWithoutAuthor() = toItemViewState().copy(author = null)

  private fun viewModel(): SeriesViewModel {
    return SeriesViewModel(
      repo = mockk<BookRepository> {
        every { flow() } returns books
      },
      navigator = navigator,
      playerController = playerController,
      currentBookStore = CurrentBookStore(currentBookStore),
      dispatcherProvider = dispatcherProvider,
      name = "Saga",
    )
  }

  private suspend fun TestScope.testState(validate: suspend ReceiveTurbine<SeriesViewState?>.() -> Unit) {
    val viewModel = viewModel()
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      // the books are still loading
      assertEquals(expected = null, actual = awaitItem())
      validate()
    }
  }
}

package voice.features.bookOverview.author

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.book
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator

class AuthorViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val dispatcherProvider =
    DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
  private val navigator = mockk<Navigator>(relaxed = true)
  private val books = MutableStateFlow<List<Book>>(emptyList())
  private val authorInfoLookup = mockk<AuthorInfoLookup>()

  init {
    coEvery { authorInfoLookup.info(any(), any()) } returns null
  }

  @Test
  fun `the books of the author are listed with their series as a stack`() = runTest {
    val first = book(name = "First", series = "Saga", part = "1", author = "Author", time = 20_000)
    val second = book(name = "Second", series = "Saga", part = "2", author = "author ", time = 0)
    val alone = book(name = "Alone", author = "Author", lastPlayedAt = Instant.ofEpochSecond(10))
    val other = book(name = "Other", author = "Someone else")
    books.value = listOf(first, second, alone, other)

    testState {
      val state = awaitItem()!!
      assertEquals(expected = "Author", actual = state.name)
      assertEquals(expected = 3, actual = state.bookCount)
      assertEquals(expected = 1, actual = state.finishedCount)
      assertEquals(
        expected =
          AuthorViewState.Entry.Book(
            book = alone.toItemViewState().copy(author = null),
            finished = false,
          ),
        actual = state.entries.first(),
      )
      val series = (state.entries[1] as AuthorViewState.Entry.Series).series
      assertEquals(expected = "Saga", actual = series.name)
      assertEquals(expected = null, actual = series.author)
      assertEquals(expected = 2, actual = series.bookCount)
      assertEquals(expected = 2, actual = state.entries.size)
    }
  }

  @Test
  fun `the info about the author shows up once it's loaded`() = runTest {
    val info =
      AuthorInfo(
        description = "American author",
        extract = "Author is an American author.",
        photo = null,
        url = "https://en.wikipedia.org/wiki/Author",
      )
    coEvery { authorInfoLookup.info("Author", any()) } returns info
    books.value = listOf(book(author = "Author"))

    testState {
      var state = awaitItem()!!
      while (state.info == null) state = awaitItem()!!
      assertEquals(expected = info, actual = state.info)
    }
  }

  @Test
  fun `the books still show when the lookup fails`() = runTest {
    coEvery { authorInfoLookup.info(any(), any()) } throws IOException()
    books.value = listOf(book(author = "Author"))

    testState {
      val state = awaitItem()!!
      assertEquals(expected = 1, actual = state.bookCount)
      assertEquals(expected = null, actual = state.info)
      expectNoEvents()
    }
  }

  @Test
  fun `the screen closes once the author has no books`() = runTest {
    books.value = listOf(book(author = "Author"))

    testState {
      assertEquals(expected = 1, actual = awaitItem()!!.bookCount)
      books.value = emptyList()
      assertEquals(expected = null, actual = awaitItem())
    }
    verify { navigator.remove(Destination.Author("Author")) }
  }

  @Test
  fun `a series opens its screen`() {
    viewModel().onSeriesClick("Saga")

    verify { navigator.goTo(Destination.Series("Saga")) }
  }

  private fun viewModel(): AuthorViewModel {
    return AuthorViewModel(
      repo =
        mockk<BookRepository> {
          every { flow() } returns books
        },
      navigator = navigator,
      serverLibrary =
        mockk {
          every { unavailableBooks } returns MutableStateFlow<Set<BookId>>(emptySet())
        },
      serverDownloads =
        mockk {
          every { states } returns MutableStateFlow<Map<BookId, BookDownloadState>>(emptyMap())
        },
      authorInfoLookup = authorInfoLookup,
      dispatcherProvider = dispatcherProvider,
      name = "Author",
    )
  }

  private suspend fun TestScope.testState(
    validate: suspend ReceiveTurbine<AuthorViewState?>.() -> Unit,
  ) {
    val viewModel = viewModel()
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.viewState()
      }
      .test {
        // the books are still loading
        assertEquals(expected = null, actual = awaitItem())
        validate()
      }
  }
}

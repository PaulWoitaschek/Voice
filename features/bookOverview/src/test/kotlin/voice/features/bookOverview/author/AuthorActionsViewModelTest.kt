package voice.features.bookOverview.author

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.book
import voice.features.bookOverview.select.BookSelectionRequests
import voice.navigation.Destination
import voice.navigation.Navigator

class AuthorActionsViewModelTest {

  private val navigator = mockk<Navigator>(relaxUnitFun = true)
  private val first = book(author = "Lena Morgan", series = "Saga", time = 0)
  private val second = book(author = "Lena Morgan", series = "Saga", time = 0)
  private val single = book(author = "Lena Morgan", time = 0)
  private val finished = book(author = "Lena Morgan", time = 20_000)
  private val books = MutableStateFlow(listOf(first, second, single, finished))
  private val repo = mockk<BookRepository> { every { flow() } returns books }
  private val bookSelectionRequests = BookSelectionRequests()
  private val destination =
    Destination.AuthorActions("Lena Morgan", listOf(first.id, second.id, single.id))
  private val viewModel =
    AuthorActionsViewModel(repo, bookSelectionRequests, navigator, destination)

  @Test
  fun `the sheet shows only the books of the entry`() = runTest {
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = null, actual = awaitItem())
        val author = awaitItem()!!
        assertEquals(expected = "Lena Morgan", actual = author.name)
        assertEquals(expected = 3, actual = author.bookCount)
        assertEquals(expected = 1, actual = author.seriesCount)
      }
  }

  @Test
  fun `select asks the screen to select the books of the entry`() = runTest {
    val requests = mutableListOf<List<BookId>>()
    backgroundScope.launch { bookSelectionRequests.requests.collect { requests += it } }
    runCurrent()

    viewModel.onSelect()
    runCurrent()

    assertEquals(expected = listOf(listOf(first.id, second.id, single.id)), actual = requests)
  }

  @Test
  fun `the sheet closes once the entry has no books`() = runTest {
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = null, actual = awaitItem())
        assertEquals(expected = 3, actual = awaitItem()!!.bookCount)

        books.value = listOf(finished)

        assertEquals(expected = null, actual = awaitItem())
      }
    verify { navigator.remove(destination) }
  }
}

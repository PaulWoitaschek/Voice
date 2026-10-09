package voice.features.bookOverview.bottomSheet

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.FakeServerLibrary
import voice.features.bookOverview.book
import voice.features.bookOverview.overview.BookOverviewCategory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class BottomSheetViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  private val slowBook = book(name = "Slow", time = 0)
  private val fastBook = book(name = "Fast", time = 1_000)

  private val repo = mockk<BookRepository> {
    coEvery { get(slowBook.id) } coAnswers {
      delay(1_000)
      slowBook
    }
    coEvery { get(fastBook.id) } returns fastBook
  }

  private val itemViewModel = object : BottomSheetItemViewModel {
    override suspend fun items(bookId: BookId): List<BottomSheetItem> = listOf(BottomSheetItem.Title)

    override suspend fun onItemClick(
      bookId: BookId,
      item: BottomSheetItem,
    ) {
    }
  }

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `selecting a book shows it with its category and items`() = runTest(dispatcher) {
    val viewModel = BottomSheetViewModel(setOf(itemViewModel), repo, FakeServerLibrary())

    viewModel.bookSelected(fastBook.id)
    advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals("Fast", state.book?.name)
    assertEquals(BookOverviewCategory.CURRENT, state.category)
    assertEquals(listOf(BottomSheetItem.Title), state.items)
  }

  @Test
  fun `selecting another book hides the previous one right away`() = runTest(dispatcher) {
    val viewModel = BottomSheetViewModel(setOf(itemViewModel), repo, FakeServerLibrary())
    viewModel.bookSelected(fastBook.id)
    advanceUntilIdle()

    viewModel.bookSelected(slowBook.id)

    assertNull(viewModel.state.value.book)
  }

  @Test
  fun `a slow load of the previous book does not replace the selected one`() = runTest(dispatcher) {
    val viewModel = BottomSheetViewModel(setOf(itemViewModel), repo, FakeServerLibrary())

    viewModel.bookSelected(slowBook.id)
    viewModel.bookSelected(fastBook.id)
    advanceUntilIdle()

    assertEquals("Fast", viewModel.state.value.book?.name)
  }
}

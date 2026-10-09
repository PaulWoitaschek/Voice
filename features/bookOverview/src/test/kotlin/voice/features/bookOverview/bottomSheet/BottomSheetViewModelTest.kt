package voice.features.bookOverview.bottomSheet

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.features.bookOverview.book
import voice.features.bookOverview.chapter
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.navigation.Destination
import voice.navigation.Navigator
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class BottomSheetViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  private val book = book(chapters = listOf(chapter(duration = 10_000), chapter(duration = 10_000)), time = 0)
  private val storedBook = MutableStateFlow<Book?>(book)
  private val livePlaybackState = MutableStateFlow<LivePlaybackState?>(null)

  private val repo = mockk<BookRepository> {
    every { flow(book.id) } returns storedBook
  }
  private val playerController = mockk<PlayerController> {
    every { livePlaybackStateFlow(book.id) } returns livePlaybackState
  }
  private val navigator = mockk<Navigator>(relaxUnitFun = true)

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
  fun `shows the book with its category and items`() = runTest(dispatcher) {
    viewModel().test {
      val state = awaitLoaded()
      assertEquals(book.content.name, state.book.name)
      assertEquals(BookOverviewCategory.NOT_STARTED, state.category)
      assertEquals(listOf(BottomSheetItem.Title), state.items)
    }
  }

  @Test
  fun `follows the book while it plays`() = runTest(dispatcher) {
    viewModel().test {
      assertEquals(0F, awaitLoaded().book.progress)

      storedBook.value = book.update { it.copy(currentChapter = book.chapters[1].id, positionInChapter = 0) }

      val state = awaitItem()!!
      assertEquals(0.5F, state.book.progress)
      assertEquals(BookOverviewCategory.CURRENT, state.category)
    }
  }

  @Test
  fun `follows the live position with the experimental persistence`() = runTest(dispatcher) {
    viewModel(experimentalPlaybackPersistence = true).test {
      assertEquals(0F, awaitLoaded().book.progress)

      livePlaybackState.value = LivePlaybackState(
        bookId = book.id,
        chapterId = book.chapters[1].id,
        positionMs = 5_000,
        isPlaying = true,
        playbackSpeed = 1F,
      )

      assertEquals(0.75F, awaitItem()!!.book.progress)
    }
  }

  @Test
  fun `a book that is gone closes the menu`() = runTest(dispatcher) {
    storedBook.value = null

    val state = viewModel()
    runCurrent()

    assertNull(state.value)
    verify { navigator.remove(Destination.BookActions(book.id)) }
  }

  @Test
  fun `an item click returns once the item is handled`() = runTest(dispatcher) {
    var handledItem: BottomSheetItem? = null
    val slowItemViewModel = object : BottomSheetItemViewModel {
      override suspend fun items(bookId: BookId): List<BottomSheetItem> = listOf(BottomSheetItem.Title)

      override suspend fun onItemClick(
        bookId: BookId,
        item: BottomSheetItem,
      ) {
        delay(1_000)
        handledItem = item
      }
    }

    bottomSheetViewModel(itemViewModels = setOf(slowItemViewModel)).onItemClick(BottomSheetItem.Title)

    assertEquals(BottomSheetItem.Title, handledItem)
  }

  @Test
  fun `closing removes the menu of the book`() = runTest(dispatcher) {
    bottomSheetViewModel().onClose()

    verify { navigator.remove(Destination.BookActions(book.id)) }
  }

  private fun bottomSheetViewModel(
    itemViewModels: Set<BottomSheetItemViewModel> = setOf(itemViewModel),
    experimentalPlaybackPersistence: Boolean = false,
  ) = BottomSheetViewModel(
    bookId = book.id,
    viewModels = itemViewModels,
    repo = repo,
    navigator = navigator,
    playerController = playerController,
    experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(experimentalPlaybackPersistence),
  )

  private fun TestScope.viewModel(experimentalPlaybackPersistence: Boolean = false): StateFlow<EditBookBottomSheetState?> {
    val viewModel = bottomSheetViewModel(experimentalPlaybackPersistence = experimentalPlaybackPersistence)
    return backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }
  }

  private suspend fun ReceiveTurbine<EditBookBottomSheetState?>.awaitLoaded(): EditBookBottomSheetState {
    while (true) {
      awaitItem()?.let { return it }
    }
  }
}

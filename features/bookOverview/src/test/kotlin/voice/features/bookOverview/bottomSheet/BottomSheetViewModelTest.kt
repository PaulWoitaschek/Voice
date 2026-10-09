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
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.features.bookOverview.FakeServerLibrary
import voice.features.bookOverview.book
import voice.features.bookOverview.chapter
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.navigation.Destination
import voice.navigation.Navigator
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BottomSheetViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  private val book = book(chapters = listOf(chapter(duration = 10_000), chapter(duration = 10_000)), time = 0)
  private val storedBook = MutableStateFlow<Book?>(book)
  private val serverBook = book.let { it.copy(content = it.content.copy(id = BookId("abs://item/1"))) }
  private val serverLibrary = FakeServerLibrary(serverName = MutableStateFlow("audiobooks.example.com"))
  private val livePlaybackState = MutableStateFlow<LivePlaybackState?>(null)

  private val repo = mockk<BookRepository> {
    every { flow(book.id) } returns storedBook
    every { flow(serverBook.id) } returns MutableStateFlow(serverBook)
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
  fun `a server book offers the download that fits its state while the sheet is open`() = runTest(dispatcher) {
    viewModel(bookId = serverBook.id).test {
      assertEquals(listOf(BottomSheetItem.Title, BottomSheetItem.Download), awaitLoaded().items)

      val downloading = BookDownloadState.Downloading(progress = 0.5F, downloadedBytes = 5, totalBytes = 10, waitingFor = null)
      serverLibrary.downloadStates.value = mapOf(serverBook.id to downloading)
      val state = awaitItem()!!
      assertEquals(listOf(BottomSheetItem.Title, BottomSheetItem.StopDownload), state.items)
      assertEquals(downloading, state.download)

      serverLibrary.downloadStates.value = mapOf(serverBook.id to BookDownloadState.Failed)
      assertEquals(
        listOf(BottomSheetItem.Title, BottomSheetItem.RetryDownload, BottomSheetItem.RemoveDownload),
        awaitItem()!!.items,
      )
    }
  }

  @Test
  fun `a book on the device shows where it comes from once a server is connected`() = runTest(dispatcher) {
    viewModel().test {
      val state = awaitLoaded()
      assertEquals(BookSource.Device, state.source)
      assertNull(state.download)
    }
  }

  @Test
  fun `stopping a download removes it`() = runTest(dispatcher) {
    val viewModel = bottomSheetViewModel(bookId = serverBook.id)

    viewModel.onItemClick(BottomSheetItem.Download)
    viewModel.onItemClick(BottomSheetItem.StopDownload)

    assertEquals(listOf(serverBook.id), serverLibrary.downloads)
    assertEquals(listOf(serverBook.id), serverLibrary.removedDownloads)
  }

  @Test
  fun `voice asks for notifications at the first download only`() = runTest(dispatcher) {
    viewModel(bookId = serverBook.id).test {
      assertTrue(awaitLoaded().askForNotifications)
    }

    bottomSheetViewModel(bookId = serverBook.id).onAskedForNotifications()
    runCurrent()

    viewModel(bookId = serverBook.id).test {
      assertFalse(awaitLoaded().askForNotifications)
    }
  }

  @Test
  fun `closing removes the menu of the book`() = runTest(dispatcher) {
    bottomSheetViewModel().onClose()

    verify { navigator.remove(Destination.BookActions(book.id)) }
  }

  private fun bottomSheetViewModel(
    itemViewModels: Set<BottomSheetItemViewModel> = setOf(itemViewModel),
    experimentalPlaybackPersistence: Boolean = false,
    bookId: BookId = book.id,
  ) = BottomSheetViewModel(
    bookId = bookId,
    viewModels = itemViewModels,
    repo = repo,
    serverLibrary = serverLibrary,
    navigator = navigator,
    playerController = playerController,
    experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(experimentalPlaybackPersistence),
  )

  private fun TestScope.viewModel(
    experimentalPlaybackPersistence: Boolean = false,
    bookId: BookId = book.id,
  ): StateFlow<EditBookBottomSheetState?> {
    val viewModel = bottomSheetViewModel(experimentalPlaybackPersistence = experimentalPlaybackPersistence, bookId = bookId)
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

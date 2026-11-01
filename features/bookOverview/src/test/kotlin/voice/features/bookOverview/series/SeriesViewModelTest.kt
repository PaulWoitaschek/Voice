package voice.features.bookOverview.series

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.features.bookOverview.MemoryDataStore
import voice.features.bookOverview.book
import voice.features.bookOverview.overview.DownloadBadge
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator

class SeriesViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
  private val navigator = mockk<Navigator>(relaxed = true)
  private val playerController = mockk<PlayerController>(relaxed = true)
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val books = MutableStateFlow<List<Book>>(emptyList())
  private val playStateManager = PlayStateManager()
  private val unavailable = MutableStateFlow<Set<BookId>>(emptySet())
  private val downloads = MutableStateFlow<Map<BookId, BookDownloadState>>(emptyMap())

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
        expected =
          listOf(
            SeriesViewState.Book(
              book = first.toItemViewStateWithoutAuthor(),
              part = "1",
              finished = true,
              current = false,
              playing = false,
            ),
            SeriesViewState.Book(
              book = second.toItemViewStateWithoutAuthor(),
              part = "2",
              finished = false,
              current = false,
              playing = false,
            ),
          ),
        actual = state.books,
      )
    }
  }

  @Test
  fun `the rows show downloads and books that are offline`() = runTest {
    val first = book(series = "Saga", part = "1")
    val second = book(series = "Saga", part = "2")
    books.value = listOf(first, second)
    unavailable.value = setOf(first.id)
    downloads.value = mapOf(second.id to BookDownloadState.Failed)

    testState {
      // the server's states arrive after the books
      awaitItem()
      val rows = awaitItem()!!.books.map { it.book }
      assertEquals(expected = listOf(true, false), actual = rows.map { it.unavailable })
      assertEquals(expected = listOf(null, DownloadBadge.Failed), actual = rows.map { it.download })
    }
  }

  @Test
  fun `the button continues the book in progress played last`() = runTest {
    val first = book(series = "Saga", part = "1", time = 1_000, lastPlayedAt = Instant.ofEpochSecond(2))
    val second = book(series = "Saga", part = "2", time = 1_000, lastPlayedAt = Instant.ofEpochSecond(1))
    books.value = listOf(first, second)

    testState {
      assertEquals(
        expected = SeriesViewState.Action(bookId = first.id, part = "1", continues = true, playing = false),
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
        expected = SeriesViewState.Action(bookId = second.id, part = null, continues = false, playing = false),
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
  fun `while a book of the series plays, the button pauses it and its row shows it`() = runTest {
    val first = book(series = "Saga", part = "1", time = 1_000, lastPlayedAt = Instant.ofEpochSecond(2))
    val second = book(series = "Saga", part = "2", time = 0)
    books.value = listOf(first, second)
    currentBookStore.updateData { second.id }
    playStateManager.playState = PlayStateManager.PlayState.Playing

    testState {
      val state = awaitItem()!!
      assertEquals(
        expected = SeriesViewState.Action(bookId = second.id, part = "2", continues = false, playing = true),
        actual = state.action,
      )
      assertEquals(expected = listOf(false to false, true to true), actual = state.books.map { it.current to it.playing })

      playStateManager.playState = PlayStateManager.PlayState.Paused
      val paused = awaitItem()!!
      assertEquals(
        expected = SeriesViewState.Action(bookId = first.id, part = "1", continues = true, playing = false),
        actual = paused.action,
      )
      assertEquals(expected = listOf(false to false, true to false), actual = paused.books.map { it.current to it.playing })
    }
  }

  @Test
  fun `with the experimental persistence, the progress follows playback`() = runTest {
    val book = book(series = "Saga", author = "Author", time = 0)
    books.value = listOf(book, book(series = "Saga", author = "Author", time = 0))
    currentBookStore.updateData { book.id }
    val livePlaybackState =
      LivePlaybackState(
        bookId = book.id,
        chapterId = book.content.currentChapter,
        positionMs = 5_000,
        isPlaying = true,
        playbackSpeed = 1F,
      )
    every { playerController.livePlaybackStateFlow(book.id) } returns flowOf(livePlaybackState)

    testState(experimentalPlaybackPersistence = true) {
      // the live position arrives after the stored one
      awaitItem()
      val row = awaitItem()!!.books.single { it.book.id == book.id }
      assertEquals(expected = book.overlay(livePlaybackState).toItemViewStateWithoutAuthor(), actual = row.book)
    }
  }

  @Test
  fun `the button plays another book and stays on the series`() = runTest {
    val book = book(series = "Saga", time = 1_000)
    currentBookStore.updateData { BookId("other") }

    viewModel().onActionClick(book.id)

    assertEquals(expected = book.id, actual = currentBookStore.data.first())
    verifyOrder {
      playerController.pauseIfCurrentBookDifferentFrom(book.id)
      playerController.play(any())
    }
    verify(exactly = 0) { navigator.goTo(any()) }
  }

  @Test
  fun `the button plays and pauses the current book`() = runTest {
    val book = book(series = "Saga", time = 1_000)
    currentBookStore.updateData { book.id }

    viewModel().onActionClick(book.id)

    verify(exactly = 1) { playerController.playPause() }
    verify(exactly = 0) { playerController.play(any()) }
    verify(exactly = 0) { navigator.goTo(any()) }
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

  private fun viewModel(experimentalPlaybackPersistence: Boolean = false): SeriesViewModel {
    return SeriesViewModel(
      repo =
        mockk<BookRepository> {
          every { flow() } returns books
        },
      navigator = navigator,
      playerController = playerController,
      currentBookStore = CurrentBookStore(currentBookStore),
      playStateManager = playStateManager,
      experimentalPlaybackPersistenceFeatureFlag =
        ExperimentalPlaybackPersistenceFeatureFlag(
          MemoryFeatureFlag(experimentalPlaybackPersistence),
        ),
      serverLibrary =
        mockk {
          every { unavailableBooks } returns unavailable
        },
      serverDownloads =
        mockk {
          every { states } returns downloads
        },
      dispatcherProvider = dispatcherProvider,
      name = "Saga",
    )
  }

  private suspend fun TestScope.testState(
    experimentalPlaybackPersistence: Boolean = false,
    validate: suspend ReceiveTurbine<SeriesViewState?>.() -> Unit,
  ) {
    val viewModel = viewModel(experimentalPlaybackPersistence)
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

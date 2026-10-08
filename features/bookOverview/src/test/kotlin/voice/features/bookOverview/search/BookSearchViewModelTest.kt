package voice.features.bookOverview.search

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.saveable.SaverScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
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
import voice.core.data.repo.internals.dao.RecentBookSearchDao
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.core.search.BookSearch
import voice.core.search.BookSearchField
import voice.core.search.BookSearchResult
import voice.features.bookOverview.MemoryDataStore
import voice.features.bookOverview.book
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BookSearchViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)

  private val shining = book(name = "The Shining", author = "Stephen King", genre = "Horror")
  private val carrie = book(name = "Carrie", author = "stephen king", genre = "Horror")
  private val kingkiller = book(name = "The Name of the Wind", author = "Patrick Rothfuss", series = "Kingkiller")

  private val recentSearches = MutableStateFlow(listOf("hail mary", "rothfuss"))
  private val recentBookSearchDao = mockk<RecentBookSearchDao> {
    every { recentBookSearches() } returns recentSearches
    coEvery { add(any()) } just Runs
    coEvery { delete(any()) } just Runs
    coEvery { clear() } just Runs
  }
  private val navigator = mockk<Navigator>(relaxed = true)
  private val playerController = mockk<PlayerController>(relaxed = true)
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val lastResults = LastBookSearchResults()
  private val search = mockk<BookSearch> {
    coEvery { search(any()) } returns emptyList()
    coEvery { search("king") } returns listOf(
      shining.result(BookSearchField.Author),
      carrie.result(BookSearchField.Author),
      kingkiller.result(BookSearchField.Series),
    )
  }

  @Test
  fun `idle shows the recent searches and browses by author`() = runTest {
    val viewModel = viewModel()

    testState(viewModel) {
      val state = awaitState<BookSearchViewState.Idle> { it.browse != null && it.recentSearches.isNotEmpty() }
      assertEquals(expected = listOf("hail mary", "rothfuss"), actual = state.recentSearches)
      assertEquals(
        expected = BookSearchViewState.Browse(
          categories = listOf(BrowseCategory.Authors, BrowseCategory.Series, BrowseCategory.Genres),
          selectedCategory = BrowseCategory.Authors,
          entries = listOf(
            BookSearchViewState.BrowseEntry(name = "Patrick Rothfuss", bookCount = 1, inProgressCount = 1, finishedCount = 0),
            // grouped regardless of the spelling
            BookSearchViewState.BrowseEntry(name = "Stephen King", bookCount = 2, inProgressCount = 2, finishedCount = 0),
          ),
        ),
        actual = state.browse,
      )
    }
  }

  @Test
  fun `browse category click shows that category`() = runTest {
    val viewModel = viewModel()

    testState(viewModel) {
      awaitState<BookSearchViewState.Idle> { it.browse != null }
      viewModel.onBrowseCategoryClick(BrowseCategory.Genres)

      val browse = awaitState<BookSearchViewState.Idle> { it.browse?.selectedCategory == BrowseCategory.Genres }.browse!!
      assertEquals(
        expected = listOf(
          BookSearchViewState.BrowseEntry(name = "Horror", bookCount = 2, inProgressCount = 2, finishedCount = 0),
        ),
        actual = browse.entries,
      )
    }
  }

  @Test
  fun `query shows the top result, the other results and the filters`() = runTest {
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("king")

      val state = awaitState<BookSearchViewState.Results>()
      assertEquals(expected = shining.id, actual = state.topResult.id)
      assertEquals(expected = listOf(carrie.id, kingkiller.id), actual = state.otherResults.map { it.id })
      assertEquals(
        expected = listOf(
          BookSearchViewState.Filter(field = null, count = 3, selected = true),
          BookSearchViewState.Filter(field = BookSearchField.Author, count = 2, selected = false),
          BookSearchViewState.Filter(field = BookSearchField.Series, count = 1, selected = false),
        ),
        actual = state.filters,
      )
      assertEquals(expected = false, actual = state.topResultPlaying)
    }
  }

  @Test
  fun `filter click narrows the results`() = runTest {
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("king")
      awaitState<BookSearchViewState.Results>()

      viewModel.onFilterClick(BookSearchField.Series)

      val state = awaitState<BookSearchViewState.Results> { it.topResult.id == kingkiller.id }
      assertEquals(expected = emptyList(), actual = state.otherResults)
      assertEquals(expected = BookSearchField.Series, actual = state.filters.single { it.selected }.field)
    }
  }

  @Test
  fun `filters are hidden when none of them narrows the results`() = runTest {
    coEvery { search.search("horror") } returns listOf(
      shining.result(BookSearchField.Genre),
      carrie.result(BookSearchField.Genre),
    )
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("horror")

      assertEquals(expected = emptyList(), actual = awaitState<BookSearchViewState.Results>().filters)
    }
  }

  @Test
  fun `clearing the query resets the filter`() = runTest {
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("king")
      awaitState<BookSearchViewState.Results>()
      viewModel.onFilterClick(BookSearchField.Series)
      awaitState<BookSearchViewState.Results> { it.topResult.id == kingkiller.id }

      viewModel.query.clearText()
      awaitState<BookSearchViewState.Idle>()
      viewModel.query.setTextAndPlaceCursorAtEnd("king")

      val state = awaitState<BookSearchViewState.Results>()
      assertEquals(expected = null, actual = state.filters.single { it.selected }.field)
      assertEquals(expected = shining.id, actual = state.topResult.id)
    }
  }

  @Test
  fun `the playing book shows its live progress`() = runTest {
    val livePlaybackState = LivePlaybackState(
      bookId = shining.id,
      chapterId = shining.chapters.last().id,
      positionMs = 5_000,
      isPlaying = true,
      playbackSpeed = 1F,
    )
    every { playerController.livePlaybackStateFlow(shining.id) } returns MutableStateFlow(livePlaybackState)
    currentBookStore.updateData { shining.id }
    val viewModel = viewModel(experimentalPlaybackPersistence = true)

    testState(viewModel) {
      viewModel.onRecentSearchClick("king")

      val expected = shining.overlay(livePlaybackState).toItemViewState()
      val state = awaitState<BookSearchViewState.Results> { it.topResult.progress == expected.progress }
      assertEquals(expected = expected.remainingTime, actual = state.topResult.remainingTime)
      assertEquals(expected = carrie.toItemViewState().progress, actual = state.otherResults.first().progress)
    }
  }

  @Test
  fun `a new view model restores the search and shows the last results right away`() = runTest {
    val viewModel = viewModel()
    testState(viewModel) {
      viewModel.onRecentSearchClick("king")
      awaitState<BookSearchViewState.Results>()
      viewModel.onFilterClick(BookSearchField.Series)
      awaitState<BookSearchViewState.Results> { it.topResult.id == kingkiller.id }
    }
    val saved = with(viewModel.saver) { SaverScope { true }.save(viewModel) }!!

    // e.g. after coming back from the player
    val restored = viewModel()
    restored.saver.restore(saved)
    testState(restored) {
      val state = awaitItem()
      assertEquals(expected = "king", actual = restored.query.text.toString())
      assertIs<BookSearchViewState.Results>(state)
      assertEquals(expected = kingkiller.id, actual = state.topResult.id)
    }
  }

  @Test
  fun `query without results shows no results`() = runTest {
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("dragonlance")

      assertEquals(
        expected = BookSearchViewState.NoResults(query = "dragonlance", filtered = false),
        actual = awaitState<BookSearchViewState.NoResults>(),
      )
    }
  }

  @Test
  fun `browse entry click searches within that category`() = runTest {
    coEvery { search.search("Stephen King") } returns listOf(
      shining.result(BookSearchField.Author),
      kingkiller.result(BookSearchField.Title),
    )
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onBrowseEntryClick(BrowseCategory.Authors, "Stephen King")

      val state = awaitState<BookSearchViewState.Results>()
      assertEquals(expected = "Stephen King", actual = viewModel.query.text.toString())
      assertEquals(expected = shining.id, actual = state.topResult.id)
      assertEquals(expected = emptyList(), actual = state.otherResults)
      coVerify { recentBookSearchDao.add("Stephen King") }
    }
  }

  @Test
  fun `book click saves the search and opens the book`() = runTest {
    val viewModel = viewModel()
    viewModel.query.edit { replace(0, length, " king ") }

    viewModel.onBookClick(shining.id)

    coVerify { recentBookSearchDao.add("king") }
    verify { navigator.goTo(Destination.Playback(shining.id)) }
  }

  @Test
  fun `play click switches to the book and plays it`() = runTest {
    currentBookStore.updateData { kingkiller.id }
    val viewModel = viewModel()

    viewModel.onPlayClick(shining.id)

    assertEquals(expected = shining.id, actual = currentBookStore.data.first())
    verifyOrder {
      playerController.pauseIfCurrentBookDifferentFrom(shining.id)
      playerController.play(any())
    }
  }

  @Test
  fun `play click on the current book toggles playback`() = runTest {
    currentBookStore.updateData { shining.id }
    val viewModel = viewModel()

    viewModel.onPlayClick(shining.id)

    verify { playerController.playPause(any()) }
    verify(exactly = 0) { playerController.play(any()) }
  }

  @Test
  fun `recent searches can be removed and cleared`() = runTest {
    val viewModel = viewModel()

    viewModel.onRemoveRecentSearch("rothfuss")
    viewModel.onClearRecentSearches()

    coVerify {
      recentBookSearchDao.delete("rothfuss")
      recentBookSearchDao.clear()
    }
  }

  private fun viewModel(experimentalPlaybackPersistence: Boolean = false): BookSearchViewModel {
    return BookSearchViewModel(
      repo = mockk<BookRepository> {
        every { flow() } returns MutableStateFlow(listOf(shining, carrie, kingkiller))
      },
      search = search,
      recentBookSearchDao = recentBookSearchDao,
      navigator = navigator,
      playerController = playerController,
      playStateManager = PlayStateManager(),
      currentBookStore = currentBookStore,
      kioskModeFeatureFlag = MemoryFeatureFlag(false),
      experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(experimentalPlaybackPersistence),
      lastResults = lastResults,
      dispatcherProvider = dispatcherProvider,
    )
  }

  private suspend fun TestScope.testState(
    viewModel: BookSearchViewModel,
    validate: suspend ReceiveTurbine<BookSearchViewState>.() -> Unit,
  ) {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test(validate = validate)
  }
}

private fun Book.result(vararg fieldsMatchingAllWords: BookSearchField): BookSearchResult {
  return BookSearchResult(
    book = this,
    matches = fieldsMatchingAllWords.associateWith { emptyList() },
    fieldsMatchingAllWords = fieldsMatchingAllWords.toSet(),
    score = 0,
  )
}

/** Skips the states on the way, e.g. the ones before the library loaded. */
@IgnorableReturnValue
private suspend inline fun <reified T : BookSearchViewState> ReceiveTurbine<BookSearchViewState>.awaitState(
  predicate: (T) -> Boolean = { true },
): T {
  while (true) {
    val state = awaitItem()
    if (state is T && predicate(state)) return state
  }
}

package voice.features.bookOverview.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.saveable.SaverScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.RecentBookSearchRepo
import voice.core.data.store.CurrentBookStore
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.core.search.BookSearch
import voice.core.search.BookSearchField
import voice.core.search.BookSearchResult
import voice.features.bookOverview.MemoryDataStore
import voice.features.bookOverview.book
import voice.features.bookOverview.overview.DownloadBadge
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator

class BookSearchViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val dispatcherProvider =
    DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)

  private val shining = book(name = "The Shining", author = "Stephen King", genre = "Horror")
  private val carrie = book(name = "Carrie", author = "stephen king", genre = "Horror")
  private val kingkiller =
    book(name = "The Name of the Wind", author = "Patrick Rothfuss", series = "Kingkiller")

  private val recentSearches = MutableStateFlow(listOf("hail mary", "rothfuss"))
  private val recentBookSearchRepo =
    mockk<RecentBookSearchRepo> {
      every { flow() } returns recentSearches
      coEvery { add(any()) } just Runs
      coEvery { delete(any()) } just Runs
      coEvery { clear() } just Runs
    }
  private val navigator = mockk<Navigator>(relaxed = true)
  private val playerController = mockk<PlayerController>(relaxed = true)
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val lastResults = LastBookSearchResults()
  private val unavailable = MutableStateFlow<Set<BookId>>(emptySet())
  private val downloads = MutableStateFlow<Map<BookId, BookDownloadState>>(emptyMap())
  private val search =
    mockk<BookSearch> {
      coEvery { search(any()) } returns emptyList()
      coEvery { search("king") } returns
        listOf(
          shining.result(BookSearchField.Author),
          carrie.result(BookSearchField.Author),
          kingkiller.result(BookSearchField.Series),
        )
    }

  @Test
  fun `query shows the top result and the other results`() = runTest {
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("king")

      val state = awaitState<BookSearchViewState.Results>()
      assertEquals(expected = shining.id, actual = state.topResult.id)
      assertEquals(
        expected = listOf(carrie.id, kingkiller.id),
        actual = state.otherResults.map { it.id },
      )
      assertEquals(expected = false, actual = state.topResultPlaying)
    }
  }

  private val herbert =
    listOf(
        "Destination: Void",
        "Dune",
        "Hellstrom's Hive",
        "The Dosadi Experiment",
        "The Green Brain",
        "The Santaroga Barrier",
        "Whipping Star",
      )
      .map { book(name = it, author = "Frank Herbert") }
  private val expanse =
    listOf("Leviathan Wakes", "Caliban's War", "Abaddon's Gate", "Cibola Burn").mapIndexed {
      index,
      name ->
      book(name = name, author = "James S. A. Corey", series = "The Expanse", part = "${index + 1}")
    }

  @Test
  fun `searching herbert shows the first result, then Frank Herbert with 7 books, then the books`() =
    runTest {
      val state = results("herbert", herbert + expanse)

      assertEquals(expected = herbert.first().id, actual = state.topResult.id)
      assertEquals(expected = listOf(author("Frank Herbert", bookCount = 7)), actual = state.rows)
      assertEquals(
        expected = herbert.drop(1).map { it.id },
        actual = state.otherResults.map { it.id },
      )
    }

  @Test
  fun `tapping the row Frank Herbert opens the author screen and puts herbert into the recent searches`() =
    runTest {
      val viewModel = viewModel(herbert)

      testState(viewModel) {
        viewModel.query.setTextAndPlaceCursorAtEnd("herbert")
        viewModel.onGroupClick(awaitState<BookSearchViewState.Results>().groups.single())
      }

      verify { navigator.goTo(Destination.Author("Frank Herbert")) }
      coVerify { recentBookSearchRepo.add("herbert") }
    }

  @Test
  fun `going back from the author screen shows herbert and its results`() = runTest {
    val viewModel = viewModel(herbert)
    lateinit var results: BookSearchViewState.Results
    testState(viewModel) {
      viewModel.query.setTextAndPlaceCursorAtEnd("herbert")
      results = awaitState<BookSearchViewState.Results>()
      viewModel.onGroupClick(results.groups.single())
    }
    val saved = with(viewModel.saver) { SaverScope { true }.save(viewModel) }!!

    // navigation disposes the search while the author screen is open
    val restored = viewModel(herbert)
    restored.saver.restore(saved)
    testState(restored) {
      assertEquals(expected = results, actual = awaitItem())
      assertEquals(expected = "herbert", actual = restored.query.text.toString())
    }
  }

  @Test
  fun `searching expanse shows the first result, then The Expanse with 4 books, then the books`() =
    runTest {
      val state = results("expanse", herbert + expanse)

      assertEquals(expected = expanse.first().id, actual = state.topResult.id)
      assertEquals(expected = listOf(series("The Expanse", bookCount = 4)), actual = state.rows)
      assertEquals(
        expected = expanse.drop(1).map { it.id },
        actual = state.otherResults.map { it.id },
      )
    }

  @Test
  fun `tapping the row The Expanse opens the series screen`() = runTest {
    val viewModel = viewModel(expanse)

    testState(viewModel) {
      viewModel.query.setTextAndPlaceCursorAtEnd("expanse")
      viewModel.onGroupClick(awaitState<BookSearchViewState.Results>().groups.single())
    }

    verify { navigator.goTo(Destination.Series("The Expanse")) }
    coVerify { recentBookSearchRepo.add("expanse") }
  }

  @Test
  fun `searching king shows the first result, then Stephen King, then Kingkiller Chronicle, then the books`() =
    runTest {
      val king = listOf("Carrie", "The Shining").map { book(name = it, author = "Stephen King") }
      val kingkiller =
        listOf("The Name of the Wind", "The Wise Man's Fear").mapIndexed { index, name ->
          book(
            name = name,
            author = "Patrick Rothfuss",
            series = "Kingkiller Chronicle",
            part = "${index + 1}",
          )
        }

      val state = results("king", kingkiller + king)

      assertEquals(expected = king.first().id, actual = state.topResult.id)
      assertEquals(
        expected =
          listOf(
            author("Stephen King", bookCount = 2),
            series("Kingkiller Chronicle", bookCount = 2),
          ),
        actual = state.rows,
      )
      assertEquals(
        expected = (king.drop(1) + kingkiller).map { it.id },
        actual = state.otherResults.map { it.id },
      )
    }

  @Test
  fun `searching herbert with one book by Frank Herbert shows the book and no row`() = runTest {
    val state = results("herbert", herbert.take(1) + expanse)

    assertEquals(expected = herbert.first().id, actual = state.topResult.id)
    assertEquals(expected = emptyList(), actual = state.groups)
    assertEquals(expected = emptyList(), actual = state.otherResults)
  }

  @Test
  fun `searching the series of one book shows the book and no row`() = runTest {
    val state = results("expanse", herbert + expanse.take(1))

    assertEquals(expected = expanse.first().id, actual = state.topResult.id)
    assertEquals(expected = emptyList(), actual = state.groups)
    assertEquals(expected = emptyList(), actual = state.otherResults)
  }

  @Test
  fun `searching herbert dune shows Dune and no row`() = runTest {
    val state = results("herbert dune", herbert)

    assertEquals(expected = "Dune", actual = state.topResult.title.text)
    assertEquals(expected = emptyList(), actual = state.groups)
    assertEquals(expected = emptyList(), actual = state.otherResults)
  }

  @Test
  fun `searching herbart shows the first result, then Frank Herbert, then the books`() = runTest {
    val state = results("herbart", herbert + expanse)

    assertEquals(expected = herbert.first().id, actual = state.topResult.id)
    assertEquals(expected = listOf(author("Frank Herbert", bookCount = 7)), actual = state.rows)
    assertEquals(
      expected = herbert.drop(1).map { it.id },
      actual = state.otherResults.map { it.id },
    )
  }

  @Test
  fun `searching s with 5 authors whose names start with s shows the first result, 3 author rows, then the books`() =
    runTest {
      val bookCounts =
        mapOf("Sam Hale" to 2, "Sara Lund" to 4, "Sid Moor" to 3, "Sol Park" to 5, "Sue Finch" to 2)
      val books = bookCounts.flatMap { (author, count) ->
        List(count) { index -> book(name = "Book $index", author = author) }
      }

      val state = results("s", books)

      assertEquals(
        expected =
          listOf(
            author("Sol Park", bookCount = 5),
            author("Sara Lund", bookCount = 4),
            author("Sid Moor", bookCount = 3),
          ),
        actual = state.rows,
      )
      assertEquals(expected = books.size - 1, actual = state.otherResults.size)
    }

  @Test
  fun `searching an author with 2 books, one hidden, shows a row that says 2 books`() = runTest {
    val books =
      listOf(
        book(name = "Carrie", author = "Stephen King"),
        book(name = "The Shining", author = "Stephen King", hidden = true),
      )

    val state = results("stephen king", books)

    assertEquals(expected = listOf(author("Stephen King", bookCount = 2)), actual = state.rows)
    assertEquals(expected = false, actual = state.topResult.hidden)
    assertEquals(expected = listOf(true), actual = state.otherResults.map { it.hidden })
  }

  @Test
  fun `results that match in titles and in chapters show the books and no chips`() = runTest {
    coEvery { search.search("league") } returns
      listOf(
        shining.result(BookSearchField.Title),
        carrie.chapterResult("The Red-Headed League", listOf(15..20)),
        kingkiller.chapterResult("The League of Five", listOf(4..9)),
      )
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("league")

      val state = awaitState<BookSearchViewState.Results>()
      assertEquals(expected = shining.id, actual = state.topResult.id)
      assertEquals(expected = emptyList(), actual = state.groups)
      assertEquals(
        expected = listOf(carrie.id, kingkiller.id),
        actual = state.otherResults.map { it.id },
      )
    }
  }

  @Test
  fun `no book matches shows No books match without Search everything`() = runTest {
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("dragonlance")

      assertEquals(
        expected = BookSearchViewState.NoResults(query = "dragonlance"),
        actual = awaitState<BookSearchViewState.NoResults>(),
      )
    }
  }

  @Test
  fun `opening search with recent searches shows only Recent`() = runTest {
    testState(viewModel()) {
      assertEquals(
        expected = BookSearchViewState.Idle(recentSearches = listOf("hail mary", "rothfuss")),
        actual = awaitState<BookSearchViewState.Idle> { it.recentSearches.isNotEmpty() },
      )
    }
  }

  @Test
  fun `opening search in kiosk mode shows a recent search`() = runTest {
    testState(viewModel(kioskMode = true)) {
      assertEquals(
        expected = BookSearchViewState.Idle(recentSearches = listOf("science fiction")),
        actual = awaitItem(),
      )
    }
  }

  @Test
  fun `4 rows match, the 3 best matches show with the authors first`() = runTest {
    val books =
      mapOf("Samuel Hale" to 3, "Samantha Lund" to 3, "Sammy Moor" to 4, "Sam" to 2).flatMap {
        (author, count) ->
        List(count) { index -> book(name = "Book $index", author = author) }
      } +
        List(2) { index ->
          book(name = "Tale $index", author = "Ann Lee", series = "Sam", part = "$index")
        }

    val state = results("sam", books)

    assertEquals(
      expected =
        listOf(
          author("Sam", bookCount = 2),
          author("Sammy Moor", bookCount = 4),
          series("Sam", bookCount = 2),
        ),
      actual = state.rows,
    )
  }

  @Test
  fun `books with a placeholder for the author show no row`() = runTest {
    val books = List(2) { index -> book(name = "Book $index", author = "Unknown") }

    val state = results("unknown", books)

    assertEquals(expected = emptyList(), actual = state.groups)
    assertEquals(expected = 1, actual = state.otherResults.size)
  }

  @Test
  fun `a row counts the books with another spelling of the author and shows the most common one`() =
    runTest {
      val books =
        listOf("J.R.R. Tolkien", "J.R.R. Tolkien", "J. R. R. Tolkien").mapIndexed { index, author ->
          book(name = "Book $index", author = author)
        }

      val state = results("tolkien", books)

      assertEquals(expected = listOf(author("J.R.R. Tolkien", bookCount = 3)), actual = state.rows)
    }

  @Test
  fun `the playing book shows its live progress`() = runTest {
    val livePlaybackState =
      LivePlaybackState(
        bookId = shining.id,
        chapterId = shining.chapters.last().id,
        positionMs = 5_000,
        isPlaying = true,
        playbackSpeed = 1F,
      )
    every { playerController.livePlaybackStateFlow(shining.id) } returns
      MutableStateFlow(livePlaybackState)
    currentBookStore.updateData { shining.id }
    val viewModel = viewModel(experimentalPlaybackPersistence = true)

    testState(viewModel) {
      viewModel.onRecentSearchClick("king")

      val expected = shining.overlay(livePlaybackState).toItemViewState()
      val state =
        awaitState<BookSearchViewState.Results> { it.topResult.progress == expected.progress }
      assertEquals(expected = expected.remainingTime, actual = state.topResult.remainingTime)
      assertEquals(
        expected = carrie.toItemViewState().progress,
        actual = state.otherResults.first().progress,
      )
    }
  }

  @Test
  fun `results show downloads and books that are offline`() = runTest {
    unavailable.value = setOf(shining.id)
    downloads.value = mapOf(carrie.id to BookDownloadState.Failed)
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("king")

      val state = awaitState<BookSearchViewState.Results>()
      assertEquals(expected = true, actual = state.topResult.unavailable)
      assertEquals(expected = shining.id, actual = state.topResult.id)
      assertEquals(expected = null, actual = state.topResult.download)
      val carrieResult = state.otherResults.first { it.id == carrie.id }
      assertEquals(expected = false, actual = carrieResult.unavailable)
      assertEquals(expected = DownloadBadge.Failed, actual = carrieResult.download)
    }
  }

  @Test
  fun `a new view model restores the search and shows the last results right away`() = runTest {
    val viewModel = viewModel()
    testState(viewModel) {
      viewModel.onRecentSearchClick("king")
      awaitState<BookSearchViewState.Results>()
    }
    val saved = with(viewModel.saver) { SaverScope { true }.save(viewModel) }!!

    // e.g. after coming back from the player
    val restored = viewModel()
    restored.saver.restore(saved)
    testState(restored) {
      val state = awaitItem()
      assertEquals(expected = "king", actual = restored.query.text.toString())
      assertIs<BookSearchViewState.Results>(state)
      assertEquals(expected = shining.id, actual = state.topResult.id)
    }
  }

  @Test
  fun `book click saves the search and opens the book`() = runTest {
    val viewModel = viewModel()
    viewModel.query.edit { replace(0, length, " king ") }

    viewModel.onBookClick(shining.id)

    coVerify { recentBookSearchRepo.add("king") }
    verify { navigator.goTo(Destination.Playback(shining.id)) }
  }

  @Test
  fun `red headed shows the book with the line Chapter The Red-Headed League`() = runTest {
    coEvery { search.search("red headed") } returns
      listOf(shining.chapterResult("The Red-Headed League", listOf(4..6, 8..13)))
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("red headed")

      assertEquals(
        expected =
          MatchTag(
            BookSearchField.Chapters,
            HighlightedText("The Red-Headed League", listOf(4..6, 8..13)),
          ),
        actual = awaitState<BookSearchViewState.Results>().topResult.matchTag,
      )
    }
  }

  @Test
  fun `a long chapter name whose match starts late shows from a few words before the match`() =
    runTest {
      val name =
        "In which our heroes travel very far across the moors and the city at night to meet the clockmaker"
      coEvery { search.search("clockmaker") } returns
        listOf(shining.chapterResult(name, listOf(87..96)))
      val viewModel = viewModel()

      testState(viewModel) {
        viewModel.onRecentSearchClick("clockmaker")

        assertEquals(
          expected =
            MatchTag(
              BookSearchField.Chapters,
              HighlightedText("…meet the clockmaker", listOf(10..19)),
            ),
          actual = awaitState<BookSearchViewState.Results>().topResult.matchTag,
        )
      }
    }

  @Test
  fun `a book that also matches in its series shows the series line`() = runTest {
    coEvery { search.search("kingkiller wind") } returns
      listOf(
        kingkiller.chapterResult("The Wind", listOf(4..7)).let { result ->
          result.copy(matches = result.matches + (BookSearchField.Series to listOf(0..9)))
        },
      )
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("kingkiller wind")

      assertEquals(
        expected = MatchTag(BookSearchField.Series, HighlightedText("Kingkiller", listOf(0..9))),
        actual = awaitState<BookSearchViewState.Results>().topResult.matchTag,
      )
    }
  }

  @Test
  fun `tapping a book found through a chapter opens the book and the place stays`() = runTest {
    coEvery { search.search("red headed") } returns
      listOf(shining.chapterResult("The Red-Headed League", listOf(4..6, 8..13)))
    val viewModel = viewModel()

    testState(viewModel) {
      viewModel.onRecentSearchClick("red headed")
      val state = awaitState<BookSearchViewState.Results>()

      viewModel.onBookClick(state.topResult.id)

      verify { navigator.goTo(Destination.Playback(shining.id)) }
      verify { playerController wasNot Called }
      assertEquals(expected = null, actual = currentBookStore.data.first())
    }
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
      recentBookSearchRepo.delete("rothfuss")
      recentBookSearchRepo.clear()
    }
  }

  private fun viewModel(
    experimentalPlaybackPersistence: Boolean = false,
    kioskMode: Boolean = false,
  ): BookSearchViewModel {
    return viewModel(
      repo = repo(listOf(shining, carrie, kingkiller)),
      search = search,
      experimentalPlaybackPersistence = experimentalPlaybackPersistence,
      kioskMode = kioskMode,
    )
  }

  /** Searches the books for real. */
  private fun viewModel(books: List<Book>): BookSearchViewModel {
    val repo = repo(books)
    return viewModel(repo = repo, search = BookSearch(repo, dispatcherProvider))
  }

  private fun repo(books: List<Book>): BookRepository = mockk {
    every { flow() } returns MutableStateFlow(books)
    coEvery { all() } returns books
  }

  private fun viewModel(
    repo: BookRepository,
    search: BookSearch,
    experimentalPlaybackPersistence: Boolean = false,
    kioskMode: Boolean = false,
  ): BookSearchViewModel {
    return BookSearchViewModel(
      repo = repo,
      search = search,
      recentBookSearchRepo = recentBookSearchRepo,
      navigator = navigator,
      playerController = playerController,
      playStateManager = PlayStateManager(),
      currentBookStore = CurrentBookStore(currentBookStore),
      kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(kioskMode)),
      experimentalPlaybackPersistenceFeatureFlag =
        ExperimentalPlaybackPersistenceFeatureFlag(
          MemoryFeatureFlag(experimentalPlaybackPersistence),
        ),
      lastResults = lastResults,
      serverLibrary =
        mockk {
          every { unavailableBooks } returns unavailable
        },
      serverDownloads =
        mockk {
          every { states } returns downloads
        },
      dispatcherProvider = dispatcherProvider,
    )
  }

  private suspend fun TestScope.results(
    query: String,
    books: List<Book>,
  ): BookSearchViewState.Results {
    val viewModel = viewModel(books)
    lateinit var results: BookSearchViewState.Results
    testState(viewModel) {
      viewModel.query.setTextAndPlaceCursorAtEnd(query)
      results = awaitState<BookSearchViewState.Results>()
    }
    return results
  }

  private suspend fun TestScope.testState(
    viewModel: BookSearchViewModel,
    validate: suspend ReceiveTurbine<BookSearchViewState>.() -> Unit,
  ) {
    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test(validate = validate)
  }
}

private data class Row(
  val kind: SearchGroupViewState.Kind,
  val name: String,
  val bookCount: Int,
)

private fun author(
  name: String,
  bookCount: Int,
) = Row(SearchGroupViewState.Kind.Author, name, bookCount)

private fun series(
  name: String,
  bookCount: Int,
) = Row(SearchGroupViewState.Kind.Series, name, bookCount)

private val BookSearchViewState.Results.rows: List<Row>
  get() = groups.map { Row(it.kind, it.name, it.bookCount) }

private fun Book.result(vararg matches: BookSearchField): BookSearchResult {
  return BookSearchResult(
    book = this,
    matches = matches.associateWith { emptyList() },
    nameScores = emptyMap(),
    score = 0,
  )
}

private fun Book.chapterResult(
  chapter: String,
  highlights: List<IntRange>,
): BookSearchResult {
  return BookSearchResult(
    book = this,
    matches = mapOf(BookSearchField.Chapters to highlights),
    nameScores = emptyMap(),
    score = 0,
    chapter = chapter,
  )
}

/** Skips the states on the way, e.g. the ones before the library loaded. */
@IgnorableReturnValue
private suspend inline fun <reified T : BookSearchViewState> ReceiveTurbine<BookSearchViewState>
  .awaitState(
  predicate: (T) -> Boolean = { true },
): T {
  while (true) {
    val state = awaitItem()
    if (state is T && predicate(state)) return state
  }
}

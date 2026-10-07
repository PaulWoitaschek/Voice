package voice.features.bookmark.history

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.features.bookmark.MemoryDataStore
import voice.features.bookmark.testBook
import voice.features.bookmark.testBookmark
import voice.features.bookmark.testChapter
import voice.features.bookmark.testEvent
import voice.features.bookmark.testZone
import voice.features.bookmark.today
import voice.features.bookmark.yesterday
import voice.navigation.Navigator
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class HistoryViewModelTest {

  private val scope = TestScope()
  private val arrival = testChapter()
  private val signal = testChapter()
  private val bookRepository = FakeBookRepository(
    testBook(listOf(arrival, signal), currentChapter = signal.id, positionInChapter = 5.minutes),
  )
  private val bookId = bookRepository.book.value.id
  private val clock = Clock.fixed(today(10), testZone)
  private val bookmarkRepo = FakeBookmarkRepo()
  private val historyRepo = FakeListeningHistoryRepo()
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val enabledStore = MemoryDataStore(true)
  private val dismissedSuggestionsStore = MemoryDataStore(emptyList<Long>())
  private val playStateManager = PlayStateManager()
  private val playerController = mockk<PlayerController>(relaxed = true)
  private val navigator = mockk<Navigator>(relaxed = true)

  private fun viewModel() = HistoryViewModel(
    currentBookStore = currentBookStore,
    bookRepository = bookRepository,
    bookmarkRepo = bookmarkRepo,
    listeningHistoryRepo = historyRepo,
    enabledStore = enabledStore,
    dismissedSuggestionsStore = dismissedSuggestionsStore,
    playStateManager = playStateManager,
    playerController = playerController,
    navigator = navigator,
    clock = clock,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
    kioskModeFeatureFlag = MemoryFeatureFlag(false),
    bookId = bookId,
  )

  private fun test(validate: suspend ReceiveTurbine<HistoryViewState?>.(HistoryViewModel) -> Unit) = scope.runTest {
    states(validate)
  }

  /** The states of a new view model, as if the screen opened. */
  private suspend fun TestScope.states(validate: suspend ReceiveTurbine<HistoryViewState?>.(HistoryViewModel) -> Unit) {
    val viewModel = viewModel()
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      validate(viewModel)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @IgnorableReturnValue
  private suspend fun ReceiveTurbine<HistoryViewState?>.awaitState(predicate: (HistoryViewState) -> Boolean = { true }): HistoryViewState {
    while (true) {
      val state = awaitItem()
      if (state != null && predicate(state)) return state
    }
  }

  private fun HistoryViewState.entries(): List<HistoryEntry> = sessions.flatMap { it.entries }

  @Test
  fun `pinning saves the spot once, even when tapped twice`() {
    historyRepo.seed(testEvent(Type.Pause, today(9), signal, 2.minutes))
    test { viewModel ->
      val pin = assertIs<HistoryAction.Pin>(awaitState().entries().single().action)
      viewModel.viewEffects.test {
        viewModel.onActionClick(pin)
        viewModel.onActionClick(pin)
        assertEquals(expected = HistoryViewEffect.Pinned, actual = awaitItem())
        scope.testScheduler.advanceUntilIdle()
        expectNoEvents()
      }
      awaitState { it.entries().single().action == null }
      val bookmark = bookmarkRepo.bookmarks.value.single()
      assertEquals(expected = signal.id, actual = bookmark.chapterId)
      assertEquals(expected = 2.minutes.inWholeMilliseconds, actual = bookmark.time)
    }
  }

  @Test
  fun `restoring brings the deleted bookmark back as it was`() {
    val bookmark = testBookmark(signal, 4.minutes, addedAt = yesterday(20), title = "Mira's message")
    historyRepo.seed(testEvent(Type.BookmarkDeleted, today(9), signal, 4.minutes, value = bookmark.title, bookmark = bookmark))
    test { viewModel ->
      val restore = assertIs<HistoryAction.Restore>(awaitState().entries().single().action)
      viewModel.viewEffects.test {
        viewModel.onActionClick(restore)
        assertEquals(expected = HistoryViewEffect.Restored, actual = awaitItem())
      }
      awaitState { it.entries().single().action == null }
      assertEquals(expected = listOf(bookmark), actual = bookmarkRepo.bookmarks.value)
    }
  }

  @Test
  fun `changing back makes the book current and sets the player`() {
    historyRepo.seed(testEvent(Type.SpeedChanged, today(9), signal, 0.minutes, value = "1.0"))
    historyRepo.seed(testEvent(Type.SpeedChanged, today(9, 5), signal, 0.minutes, value = "1.8"))
    test { viewModel ->
      val changeBack = assertIs<HistoryAction.ChangeBack>(awaitState().entries().first().action)
      viewModel.viewEffects.test {
        viewModel.onActionClick(changeBack)
        assertEquals(expected = HistoryViewEffect.ChangedBack, actual = awaitItem())
      }
      assertEquals(expected = bookId, actual = currentBookStore.data.first())
      verify { playerController.setSpeed(1F) }
    }
  }

  @Test
  fun `jumping back goes there, keeps playing and closes the screen`() {
    playStateManager.playState = PlayStateManager.PlayState.Playing
    historyRepo.seed(testEvent(Type.Seek, today(9, 55), signal, 1.minutes, to = signal to 8.minutes))
    test { viewModel ->
      val jumpBack = assertIs<HistoryAction.JumpBack>(awaitState().entries().single().action)
      viewModel.onActionClick(jumpBack)
      scope.testScheduler.advanceUntilIdle()
      assertEquals(expected = bookId, actual = currentBookStore.data.first())
      verify {
        playerController.setPosition(1.minutes.inWholeMilliseconds, signal.id, Type.JumpBack)
        playerController.play()
        navigator.goBack()
      }
    }
  }

  @Test
  fun `going to the last touch records a seek and closes the screen`() {
    test { viewModel ->
      awaitState()
      viewModel.onActionClick(HistoryAction.GoThere(arrival.id, 3.minutes.inWholeMilliseconds))
      scope.testScheduler.advanceUntilIdle()
      verify {
        playerController.setPosition(3.minutes.inWholeMilliseconds, arrival.id, Type.Seek)
        navigator.goBack()
      }
      verify(exactly = 0) { playerController.play() }
    }
  }

  @Test
  fun `a kept suggestion stays hidden when the screen opens again`() {
    playStateManager.playState = PlayStateManager.PlayState.Playing
    historyRepo.seed(testEvent(Type.Play, today(9, 30), signal, 5.minutes, source = Source.Car))
    scope.runTest {
      states { viewModel ->
        val suggestion = assertIs<HistorySuggestion.StartedBy>(awaitState().suggestion)
        viewModel.onSuggestionKeep(suggestion)
        awaitState { it.suggestion == null }
        assertEquals(expected = listOf(suggestion.key), actual = dismissedSuggestionsStore.data.first())
      }
      states {
        assertNull(awaitState().suggestion)
      }
    }
  }

  @Test
  fun `going back from a suggestion jumps there and hides it`() {
    historyRepo.seed(testEvent(Type.ChapterChange, today(9, 55), signal, 5.minutes, to = arrival to 0.minutes))
    test { viewModel ->
      val suggestion = assertIs<HistorySuggestion.Jumped>(awaitState().suggestion)
      viewModel.onSuggestionBack(suggestion)
      awaitState { it.suggestion == null }
      verify {
        playerController.setPosition(5.minutes.inWholeMilliseconds, signal.id, Type.JumpBack)
        navigator.goBack()
      }
    }
  }

  @Test
  fun `the ongoing session follows the position`() {
    playStateManager.playState = PlayStateManager.PlayState.Playing
    historyRepo.seed(testEvent(Type.Play, today(9, 30), arrival, 0.minutes))
    test {
      assertEquals(expected = 0.75F, actual = awaitState { it.sessions.single().barEnd > 0F }.sessions.single().barEnd)
      bookRepository.book.update { book -> book.update { it.copy(positionInChapter = 9.minutes.inWholeMilliseconds) } }
      awaitState { it.sessions.single().barEnd == 0.95F }
    }
  }

  @Test
  fun `the history can be turned on`() {
    scope.runTest {
      enabledStore.updateData { false }
      states { viewModel ->
        awaitState { !it.enabled }
        viewModel.onTurnOnClick()
        awaitState { it.enabled }
        assertTrue(enabledStore.data.first())
      }
    }
  }
}

private class FakeBookRepository(initial: Book) : BookRepository {

  val book = MutableStateFlow(initial)

  override fun flow(): Flow<List<Book>> = book.map { listOf(it) }

  override suspend fun all(): List<Book> = listOf(book.value)

  override fun flow(id: BookId): Flow<Book?> = book.map { book -> book.takeIf { it.id == id } }

  override suspend fun get(id: BookId): Book? = book.value.takeIf { it.id == id }

  override suspend fun updateBook(
    id: BookId,
    update: (BookContent) -> BookContent,
  ) {
    book.update { book -> if (book.id == id) book.update(update) else book }
  }
}

private class FakeBookmarkRepo : BookmarkRepo {

  val bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

  override suspend fun deleteBookmark(id: Bookmark.Id) {
    bookmarks.update { bookmarks -> bookmarks.filterNot { it.id == id } }
  }

  override suspend fun addBookmark(bookmark: Bookmark) {
    bookmarks.update { bookmarks -> bookmarks.filterNot { it.id == bookmark.id } + bookmark }
  }

  override suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
    setBySleepTimer: Boolean,
  ): Bookmark = error("Not used by the history")

  override suspend fun bookmarks(book: BookContent): List<Bookmark> = bookmarks.value

  override fun bookmarksFlow(book: BookContent): Flow<List<Bookmark>> = bookmarks
}

private class FakeListeningHistoryRepo : ListeningHistoryRepo {

  private val events = MutableStateFlow<List<ListeningEvent>>(emptyList())

  override fun events(bookId: BookId): Flow<List<ListeningEvent>> = events.map { events ->
    events.filter { it.bookId == bookId }.sortedByDescending { it.atMillis }
  }

  override suspend fun add(event: ListeningEvent) {
    seed(event)
  }

  fun seed(vararg events: ListeningEvent) {
    this.events.update { it + events }
  }

  override suspend fun clear() {
    events.value = emptyList()
  }
}

package voice.features.bookmark.history

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.features.bookmark.MemoryDataStore
import voice.features.bookmark.chapterTime
import voice.features.bookmark.testBook
import voice.features.bookmark.testChapter
import voice.features.bookmark.testEvent
import voice.features.bookmark.testZone
import voice.features.bookmark.today
import voice.navigation.Navigator
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class HistoryViewModelTest {

  private val scope = TestScope()
  private val arrival = testChapter()
  private val signal = testChapter()
  private val book = MutableStateFlow(
    testBook(listOf(arrival, signal), currentChapter = signal.id, positionInChapter = 5.minutes),
  )
  private val bookId = book.value.id
  private val bookRepository = mockk<BookRepository> {
    every { flow(bookId) } returns book
    coEvery { get(bookId) } answers { book.value }
  }
  private val clock = Clock.fixed(today(10), testZone)
  private val historyEvents = mutableListOf<ListeningEvent>()
  private val historyRepo = mockk<ListeningHistoryRepo> {
    every { events(bookId) } answers { flowOf(historyEvents.sortedByDescending { it.atMillis }) }
  }
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val playStateManager = PlayStateManager()
  private val playerController = mockk<PlayerController>(relaxed = true)
  private val navigator = mockk<Navigator>(relaxed = true)

  private fun viewModel() = HistoryViewModel(
    currentBookStore = currentBookStore,
    bookRepository = bookRepository,
    listeningHistoryRepo = historyRepo,
    playStateManager = playStateManager,
    playerController = playerController,
    navigator = navigator,
    clock = clock,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
    kioskModeFeatureFlag = MemoryFeatureFlag(false),
    bookId = bookId,
  )

  private fun test(validate: suspend ReceiveTurbine<HistoryViewState?>.(HistoryViewModel) -> Unit) = scope.runTest {
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

  private fun HistoryViewState.single(): HistoryStretch = days.single().rows.filterIsInstance<HistoryStretch>().single()

  private fun listenedUntilPause() {
    historyEvents += testEvent(Type.Play, today(9), arrival, 1.minutes)
    historyEvents += testEvent(Type.Pause, today(9, 20), signal, 2.minutes)
  }

  @Test
  fun `tapping a stretch goes where it ended, keeps playing and closes the screen`() {
    playStateManager.playState = PlayStateManager.PlayState.Playing
    listenedUntilPause()
    test { viewModel ->
      viewModel.onGoTo(awaitState().single().goTo!!.spot)
      scope.testScheduler.advanceUntilIdle()
      assertEquals(expected = bookId, actual = currentBookStore.data.first())
      verify {
        playerController.setPosition(2.minutes.inWholeMilliseconds, signal.id, Type.Seek)
        playerController.play()
        navigator.goBack()
      }
    }
  }

  @Test
  fun `going there while paused doesn't start playback`() {
    listenedUntilPause()
    test { viewModel ->
      viewModel.onGoTo(awaitState().single().goTo!!.spot)
      scope.testScheduler.advanceUntilIdle()
      verify { playerController.setPosition(2.minutes.inWholeMilliseconds, signal.id, Type.Seek) }
      verify(exactly = 0) { playerController.play() }
    }
  }

  @Test
  fun `a double tap goes there once`() {
    listenedUntilPause()
    test { viewModel ->
      val spot = awaitState().single().goTo!!.spot
      viewModel.onGoTo(spot)
      viewModel.onGoTo(spot)
      scope.testScheduler.advanceUntilIdle()
      verify(exactly = 1) {
        playerController.setPosition(2.minutes.inWholeMilliseconds, signal.id, Type.Seek)
        navigator.goBack()
      }
    }
  }

  @Test
  fun `going back from a jump goes to where it left`() {
    historyEvents += testEvent(Type.ChapterChange, today(9), arrival, 4.minutes, source = Source.Headset, to = signal to 0.minutes)
    test { viewModel ->
      val jump = awaitState().days.single().rows.filterIsInstance<HistoryJump>().single()
      viewModel.onGoTo(jump.back.spot)
      scope.testScheduler.advanceUntilIdle()
      verify {
        playerController.setPosition(4.minutes.inWholeMilliseconds, arrival.id, Type.Seek)
        navigator.goBack()
      }
    }
  }

  @Test
  fun `another book playing doesn't make this book's last stretch play on`() {
    playStateManager.playState = PlayStateManager.PlayState.Playing
    historyEvents += testEvent(Type.Play, today(9, 30), arrival, 0.minutes)
    test {
      currentBookStore.updateData { BookId("other") }
      val stretch = awaitState().single()
      assertNull(stretch.end)
      assertEquals(expected = HistorySpot(arrival.id, 0L), actual = stretch.goTo?.spot)
    }
  }

  @Test
  fun `the playing stretch follows the position`() {
    playStateManager.playState = PlayStateManager.PlayState.Playing
    historyEvents += testEvent(Type.Play, today(9, 30), arrival, 0.minutes)
    test {
      currentBookStore.updateData { bookId }
      awaitState { it.single().to == HistoryLocation(2, chapterTime(5.minutes)) }
      book.update { it.update { content -> content.copy(positionInChapter = 9.minutes.inWholeMilliseconds) } }
      awaitState { it.single().to == HistoryLocation(2, chapterTime(9.minutes)) }
    }
  }
}

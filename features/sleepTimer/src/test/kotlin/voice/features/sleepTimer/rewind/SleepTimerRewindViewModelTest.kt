package voice.features.sleepTimer.rewind

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.core.data.BookId
import voice.core.data.sleeptimer.SleepTimerRewind
import voice.core.playback.PlayerController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class SleepTimerRewindViewModelTest {

  private val scope = TestScope()
  private val book = book(chapterDurations = listOf(20.minutes, 40.minutes))
  private val rewind = SleepTimerRewind(
    bookId = book.id,
    startPositionInBookMs = 15.minutes.inWholeMilliseconds,
    timerDuration = 30.minutes,
    playbackSpeed = 1F,
  )
  private val rewindStore = MemoryDataStore<SleepTimerRewind?>(rewind)
  private val currentBookStore = MemoryDataStore<BookId?>(book.id)
  private val player = mockk<PlayerController> {
    every { setPosition(any(), any()) } just Runs
  }

  private val viewModel = SleepTimerRewindViewModel(
    sleepTimerRewindStore = rewindStore,
    currentBookStore = currentBookStore,
    bookRepository = mockk {
      coEvery { get(book.id) } returns book
    },
    player = player,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun `offers rewind options for the current book`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      // null while the stores load
      var viewState = awaitItem()
      while (viewState == null) viewState = awaitItem()
      assertEquals(
        expected = listOf(30, 25, 20, 15, 10, 5).map { it.minutes },
        actual = viewState.options.map { it.amount },
      )
    }
  }

  @Test
  fun `no prompt when another book is current`() = scope.runTest {
    currentBookStore.updateData { BookId("other") }

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertNull(expectMostRecentItem())
    }
  }

  @Test
  fun `rewinding seeks across chapters and clears the prompt`() = scope.runTest {
    // 10 minutes back from the end: 15 min start + 20 min listened = 35 min in the book, 15 min into the second chapter
    val option = rewind.rewindOptions().single { it.amount == 10.minutes }

    viewModel.onRewind(option)
    runCurrent()

    verify(exactly = 1) {
      player.setPosition(time = 15.minutes.inWholeMilliseconds, id = book.chapters[1].id)
    }
    assertNull(rewindStore.data.first())
  }

  @Test
  fun `rewinding to the timer start goes back to where the timer started`() = scope.runTest {
    viewModel.onRewind(rewind.rewindOptions().first())
    runCurrent()

    verify(exactly = 1) {
      player.setPosition(time = 15.minutes.inWholeMilliseconds, id = book.chapters[0].id)
    }
  }

  @Test
  fun `dismissing clears the prompt without seeking`() = scope.runTest {
    viewModel.onDismiss()
    runCurrent()

    assertNull(rewindStore.data.first())
    verify(exactly = 0) { player.setPosition(any(), any()) }
  }
}

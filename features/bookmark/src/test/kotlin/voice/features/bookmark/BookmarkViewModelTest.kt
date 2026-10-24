package voice.features.bookmark

import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.withCompositionLocal
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ListeningEvent
import voice.core.data.store.CurrentBookStore
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.LastPlaybackSpeedStore
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.SleepTimerState
import voice.core.ui.formatTime
import voice.navigation.Navigator
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BookmarkViewModelTest {

  private val scope = TestScope()
  private val arrival = testChapter()
  private val signal = testChapter()
  private val book = testBook(listOf(arrival, signal), currentChapter = signal.id, positionInChapter = 5.minutes)
  private val clock = Clock.fixed(today(10), testZone)
  private val bookmarkRepo = FakeBookmarkRepo(clock)
  private val playStateManager = PlayStateManager()
  private val livePlaybackState = MutableStateFlow<LivePlaybackState?>(null)
  private val playerController = mockk<PlayerController>(relaxed = true) {
    every { livePlaybackStateFlow(book.id) } returns livePlaybackState
  }
  private val navigator = mockk<Navigator> {
    every { goBack() } just Runs
  }

  private fun viewModel(
    editBookmarkId: String? = null,
    experimentalPlaybackPersistence: Boolean = false,
    lastPlaybackSpeed: Float = 1F,
    historyTabOpenStore: MemoryDataStore<Boolean> = MemoryDataStore(true),
  ) = BookmarkViewModel(
    currentBookStore = CurrentBookStore(MemoryDataStore<BookId?>(null)),
    bookRepository = mockk {
      every { flow(book.id) } returns flowOf(book)
    },
    bookmarkRepo = bookmarkRepo,
    currentBookResolver = mockk {
      coEvery { book(book.id) } returns book
    },
    playStateManager = playStateManager,
    playerController = playerController,
    sleepTimer = mockk {
      every { state } returns MutableStateFlow(SleepTimerState.Disabled)
    },
    navigator = navigator,
    clock = clock,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
    kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(false)),
    experimentalPlaybackPersistenceFeatureFlag = ExperimentalPlaybackPersistenceFeatureFlag(
      MemoryFeatureFlag(experimentalPlaybackPersistence),
    ),
    lastPlaybackSpeedStore = LastPlaybackSpeedStore(MemoryDataStore(lastPlaybackSpeed)),
    historyTabOpenStore = HistoryTabOpenStore(historyTabOpenStore),
    bookId = book.id,
    editBookmarkId = editBookmarkId,
  )

  private fun BookmarkViewModel.test(validate: suspend ReceiveTurbine<BookmarkViewState?>.() -> Unit) = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewState()
    }.test {
      validate()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @IgnorableReturnValue
  private suspend fun ReceiveTurbine<BookmarkViewState?>.awaitState(
    predicate: (BookmarkViewState) -> Boolean = { true },
  ): BookmarkViewState {
    while (true) {
      val state = awaitItem()
      if (state != null && predicate(state)) return state
    }
  }

  @Test
  fun `the screen opens on the history at first`() {
    viewModel().test {
      assertEquals(expected = BookmarkTab.History, actual = awaitState().tab)
    }
  }

  @Test
  fun `picking a tab keeps it for next time`() {
    val store = MemoryDataStore(true)
    val viewModel = viewModel(historyTabOpenStore = store)
    viewModel.test {
      awaitState()
      viewModel.onTabChange(BookmarkTab.Bookmarks)
      awaitState { it.tab == BookmarkTab.Bookmarks }
      scope.testScheduler.advanceUntilIdle()
      assertEquals(expected = false, actual = store.data.first())
    }
  }

  @Test
  fun `the screen opens on the bookmarks when they were open last`() {
    viewModel(historyTabOpenStore = MemoryDataStore(false)).test {
      assertEquals(expected = BookmarkTab.Bookmarks, actual = awaitState().tab)
    }
  }

  @Test
  fun `a bookmark to edit opens on the bookmarks`() {
    val bookmark = testBookmark(arrival, 1.minutes)
    bookmarkRepo.bookmarks.value = listOf(bookmark)
    viewModel(editBookmarkId = bookmark.id.value.toString()).test {
      assertEquals(expected = BookmarkTab.Bookmarks, actual = awaitState().tab)
    }
  }

  @Test
  fun `saving keeps the bookmark and opens its details`() {
    val viewModel = viewModel()
    viewModel.test {
      awaitState()
      viewModel.onSaveClick()
      val editor = awaitState { it.editor != null }.editor!!
      assertTrue(editor.isNew)
      assertEquals(expected = 2, actual = editor.chapterNumber)
      assertEquals(expected = chapterTime(5.minutes), actual = editor.time)
      assertEquals(expected = listOf(editor.id), actual = bookmarkRepo.bookmarks.value.map { it.id })
    }
  }

  @Test
  fun `details are saved when done`() {
    val viewModel = viewModel()
    viewModel.test {
      awaitState()
      viewModel.onSaveClick()
      awaitState { it.editor != null }
      viewModel.onMoveEarlier()
      viewModel.onMoveEarlier()
      viewModel.onNoteChange("  Mira finds the signal ")
      viewModel.onKindChange(Bookmark.Kind.Quote)
      assertEquals(expected = chapterTime(4.5.minutes), actual = awaitState { it.editor?.kind == Bookmark.Kind.Quote }.editor?.time)
      viewModel.onEditorDone()
      awaitState { it.editor == null && it.items.any { item -> item is BookmarkListItem.Row && item.bookmark.note != null } }
      val saved = bookmarkRepo.bookmarks.value.single()
      assertEquals(expected = 4.5.minutes.inWholeMilliseconds, actual = saved.time)
      assertEquals(expected = "Mira finds the signal", actual = saved.title)
      assertEquals(expected = Bookmark.Kind.Quote, actual = saved.kind)
    }
  }

  @Test
  fun `undo removes the bookmark just saved`() {
    val viewModel = viewModel()
    viewModel.test {
      awaitState()
      viewModel.onSaveClick()
      awaitState { it.editor != null }
      viewModel.onEditorUndo()
      awaitState { it.editor == null && it.totalCount == 0 }
      assertEquals(expected = emptyList(), actual = bookmarkRepo.bookmarks.value)
    }
  }

  @Test
  fun `a deleted bookmark can be brought back`() {
    val bookmark = testBookmark(signal, 2.minutes)
    bookmarkRepo.bookmarks.value = listOf(bookmark)
    val viewModel = viewModel()
    viewModel.test {
      awaitState { it.totalCount == 1 }
      viewModel.viewEffects.test {
        viewModel.onDelete(bookmark.id)
        assertEquals(expected = BookmarkViewEffect.Deleted(bookmark), actual = awaitItem())
      }
      awaitState { it.totalCount == 0 }
      viewModel.onUndoDelete(bookmark)
      awaitState { it.totalCount == 1 }
      assertEquals(expected = listOf(bookmark), actual = bookmarkRepo.bookmarks.value)
    }
  }

  @Test
  fun `deleting quickly in a row keeps the undo for each`() {
    val bookmarks = List(3) { testBookmark(signal, (it + 1).minutes) }
    bookmarkRepo.bookmarks.value = bookmarks
    val viewModel = viewModel()
    viewModel.test {
      awaitState { it.totalCount == 3 }
      val shown = mutableListOf<BookmarkViewEffect>()
      scope.backgroundScope.launch {
        viewModel.viewEffects.collect { effect ->
          shown += effect
          // the snackbar stays until it times out
          delay(4.seconds)
        }
      }
      bookmarks.forEach { viewModel.onDelete(it.id) }
      awaitState { it.totalCount == 0 }
      // advanceUntilIdle doesn't run the background collector
      scope.testScheduler.advanceTimeBy(1.minutes)
      assertEquals<List<BookmarkViewEffect>>(expected = bookmarks.map { BookmarkViewEffect.Deleted(it) }, actual = shown)
    }
  }

  @Test
  fun `saving shows all kinds again, so the new bookmark is not filtered away`() {
    bookmarkRepo.bookmarks.value = listOf(testBookmark(arrival, 1.minutes, kind = Bookmark.Kind.Favorite))
    val viewModel = viewModel()
    viewModel.test {
      awaitState { it.totalCount == 1 }
      viewModel.onCategoryClick(Bookmark.Kind.Favorite)
      awaitState { it.selectedCategory == Bookmark.Kind.Favorite }
      viewModel.onSaveClick()
      val state = awaitState { it.editor != null && it.totalCount == 2 }
      assertNull(state.selectedCategory)
      assertTrue(state.items.any { it is BookmarkListItem.Row && it.bookmark.id == state.editor?.id })
    }
  }

  @Test
  fun `a double tap saves the moment once`() {
    val viewModel = viewModel()
    viewModel.test {
      awaitState()
      viewModel.onSaveClick()
      viewModel.onSaveClick()
      awaitState { it.editor != null }
      scope.testScheduler.advanceUntilIdle()
      assertEquals(expected = 1, actual = bookmarkRepo.bookmarks.value.size)
    }
  }

  @Test
  fun `a double tap on a bookmark jumps and goes back once`() {
    val bookmark = testBookmark(arrival, 2.minutes)
    bookmarkRepo.bookmarks.value = listOf(bookmark)
    val viewModel = viewModel()
    viewModel.test {
      awaitState { it.totalCount == 1 }
      viewModel.onBookmarkClick(bookmark.id)
      scope.testScheduler.advanceUntilIdle()
      viewModel.onBookmarkClick(bookmark.id)
      viewModel.onCloseClick()
      scope.testScheduler.advanceUntilIdle()
      verify(exactly = 1) {
        playerController.setPosition(any(), any(), any())
        navigator.goBack()
      }
    }
  }

  @Test
  fun `the time left of a book that was never played is at the last speed`() {
    viewModel(lastPlaybackSpeed = 2F).test {
      // 5 of 20 minutes are left
      val state = awaitState { it.bookBar != null }
      assertEquals(expected = formatTime(2.5.minutes.inWholeMilliseconds), actual = state.bookBar!!.remaining)
    }
  }

  @Test
  fun `with the experimental playback persistence, the position is the player's`() {
    val viewModel = viewModel(experimentalPlaybackPersistence = true)
    viewModel.onSortChange(BookmarkSort.Story)
    viewModel.test {
      // the stored position: 5 minutes into the second of two 10 minute chapters
      awaitState { it.bookBar?.percent == 75 }
      livePlaybackState.value = LivePlaybackState(
        bookId = book.id,
        chapterId = arrival.id,
        positionMs = 2.minutes.inWholeMilliseconds,
        isPlaying = true,
        playbackSpeed = 1F,
      )
      val state = awaitState { it.bookBar?.percent == 10 }
      assertTrue(state.playing)
      val youAreHere = state.items.filterIsInstance<BookmarkListItem.YouAreHere>().single()
      assertEquals(expected = chapterTime(2.minutes), actual = youAreHere.time)
    }
  }

  @Test
  fun `bookmarks start with the newest`() {
    val older = testBookmark(arrival, 2.minutes, addedAt = today(8))
    val newer = testBookmark(arrival, 1.minutes, addedAt = today(9))
    bookmarkRepo.bookmarks.value = listOf(older, newer)
    viewModel().test {
      val rows = awaitState { it.items.size == 2 }.items.filterIsInstance<BookmarkListItem.Row>()
      assertEquals(expected = listOf(newer.id, older.id), actual = rows.map { it.bookmark.id })
    }
  }

  @Test
  fun `clicking a bookmark jumps there and keeps playing`() {
    val bookmark = testBookmark(arrival, 2.minutes)
    bookmarkRepo.bookmarks.value = listOf(bookmark)
    playStateManager.playState = PlayStateManager.PlayState.Playing
    val viewModel = viewModel()
    viewModel.test {
      awaitState { it.totalCount == 1 }
      viewModel.onBookmarkClick(bookmark.id)
      scope.testScheduler.advanceUntilIdle()
      verify {
        playerController.setPosition(2.minutes.inWholeMilliseconds, arrival.id, ListeningEvent.Type.BookmarkJump)
        playerController.play()
        navigator.goBack()
      }
    }
  }

  @Test
  fun `opening for a bookmark shows its details`() {
    val bookmark = testBookmark(signal, 2.minutes, title = "Static")
    bookmarkRepo.bookmarks.value = listOf(bookmark)
    val viewModel = viewModel(editBookmarkId = bookmark.id.value.toString())
    viewModel.test {
      val editor = awaitState { it.editor != null }.editor!!
      assertEquals(expected = false, actual = editor.isNew)
      assertEquals(expected = "Static", actual = editor.note)
    }
  }

  @Test
  fun `the details opened for a bookmark stay closed once the screen comes back`() = scope.runTest {
    val bookmark = testBookmark(signal, 2.minutes, title = "Static")
    bookmarkRepo.bookmarks.value = listOf(bookmark)
    val editBookmarkId = bookmark.id.value.toString()
    val registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
    viewModel(editBookmarkId = editBookmarkId).states(registry).test {
      awaitState { it.editor != null }
      cancelAndIgnoreRemainingEvents()
    }

    // as after process death: a new view model, with what the screen saved
    val restored = SaveableStateRegistry(restoredValues = registry.performSave(), canBeSaved = { true })
    viewModel(editBookmarkId = editBookmarkId).states(restored).test {
      assertNull(awaitState().editor)
      testScheduler.runCurrent()
      expectNoEvents()
    }
  }

  private fun BookmarkViewModel.states(registry: SaveableStateRegistry) =
    scope.backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      withCompositionLocal(LocalSaveableStateRegistry provides registry) {
        viewState()
      }
    }

  @Test
  fun `nudging stops at the start of the chapter`() {
    val bookmark = testBookmark(signal, 10.seconds)
    bookmarkRepo.bookmarks.value = listOf(bookmark)
    val viewModel = viewModel()
    viewModel.test {
      awaitState { it.totalCount == 1 }
      viewModel.onBookmarkLongClick(bookmark.id)
      awaitState { it.editor?.canMoveEarlier == true }
      viewModel.onMoveEarlier()
      val editor = awaitState { it.editor?.canMoveEarlier == false }.editor!!
      assertEquals(expected = chapterTime(0.seconds), actual = editor.time)
      assertNull(editor.chapterName)
    }
  }
}

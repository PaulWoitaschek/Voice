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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookmarkRepo
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerState
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
  ) = BookmarkViewModel(
    currentBookStore = MemoryDataStore<BookId?>(null),
    bookRepository = mockk {
      every { flow(book.id) } returns flowOf(book)
    },
    bookmarkRepo = bookmarkRepo,
    currentBookResolver = mockk {
      coEvery { book(book.id) } returns book
    },
    playStateManager = playStateManager,
    playerController = playerController,
    sleepTimer = DisabledSleepTimer,
    navigator = navigator,
    clock = clock,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
    kioskModeFeatureFlag = MemoryFeatureFlag(false),
    experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(experimentalPlaybackPersistence),
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
      viewModel.onCategoryClick(BookmarkCategory.Favorite)
      awaitState { it.selectedCategory == BookmarkCategory.Favorite }
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
  fun `with the experimental playback persistence, the position is the player's`() {
    val viewModel = viewModel(experimentalPlaybackPersistence = true)
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
  fun `a sleep timer bookmark can be made dozed off again`() {
    val bookmark = testBookmark(signal, 2.minutes, setBySleepTimer = true)
    bookmarkRepo.bookmarks.value = listOf(bookmark)
    val viewModel = viewModel()
    viewModel.test {
      awaitState { it.totalCount == 1 }
      viewModel.onBookmarkLongClick(bookmark.id)
      awaitState { it.editor?.setBySleepTimer == true }
      viewModel.onKindChange(Bookmark.Kind.Favorite)
      val favorite = awaitState { it.editor?.setBySleepTimer == false }.editor!!
      assertTrue(favorite.wasSetBySleepTimer)
      viewModel.onSleepKindClick()
      awaitState { it.editor?.setBySleepTimer == true }
      viewModel.onEditorDone()
      awaitState { it.editor == null }
      assertEquals(expected = listOf(bookmark), actual = bookmarkRepo.bookmarks.value)
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

private object DisabledSleepTimer : SleepTimer {
  override val state = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)
  override fun enable(mode: SleepTimerMode) {}
  override fun disable() {}
}

private class FakeBookmarkRepo(private val clock: Clock) : BookmarkRepo {

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
  ): Bookmark {
    val bookmark = Bookmark(
      bookId = book.id,
      chapterId = book.content.currentChapter,
      title = title,
      time = book.content.positionInChapter,
      addedAt = clock.instant(),
      setBySleepTimer = setBySleepTimer,
      id = Bookmark.Id.random(),
    )
    addBookmark(bookmark)
    return bookmark
  }

  override suspend fun bookmarks(book: BookContent): List<Bookmark> = bookmarks.value

  override fun bookmarksFlow(book: BookContent): Flow<List<Bookmark>> = bookmarks
}

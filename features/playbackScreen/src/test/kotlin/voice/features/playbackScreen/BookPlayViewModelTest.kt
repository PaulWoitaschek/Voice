package voice.features.playbackScreen

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.KioskModeDemoData
import voice.core.data.ListeningEvent
import voice.core.data.MarkData
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.CurrentBookResolver
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.history.ListeningHistoryRecorder
import voice.core.playback.history.PlaybackPosition
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerMode.TimedWithDuration
import voice.core.sleeptimer.SleepTimerState
import voice.core.ui.BookBarPin
import voice.features.sleepTimer.SleepTimerViewState
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class BookPlayViewModelTest {

  private val scope = TestScope()
  private val clock = TestClock()
  private val historyRecorder = ListeningHistoryRecorder(repo = mockk(relaxed = true), clock = clock, scope = scope)
  private val sleepTimerDataStore = MemoryDataStore(SleepTimerPreference.Default.copy(duration = 5.minutes))
  private val book = book()
  private val sleepTimer = mockk<SleepTimer> {
    val stateFlow = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)
    every {
      state
    } returns stateFlow
    every {
      enable(any())
    } answers {
      stateFlow.value = when (val mode = firstArg<SleepTimerMode>()) {
        is TimedWithDuration -> SleepTimerState.Enabled.WithDuration(mode.duration)
        SleepTimerMode.TimedWithDefault -> SleepTimerState.Enabled.WithDuration(runBlocking { sleepTimerDataStore.data.first() }.duration)
        SleepTimerMode.EndOfChapter -> SleepTimerState.Enabled.WithEndOfChapter
      }
    }
    every {
      disable()
    } answers {
      stateFlow.value = SleepTimerState.Disabled
    }
  }

  private val player = mockk<PlayerController>()
  private val playStateManager = mockk<PlayStateManager> {
    every { playStateFlow } returns MutableStateFlow(PlayStateManager.PlayState.Paused)
  }
  private val currentBookStoreId = MemoryDataStore<BookId?>(null)
  private val currentBookResolver = mockk<CurrentBookResolver> {
    coEvery { book(book.id) } returns book
  }
  private val viewModel = BookPlayViewModel(
    bookRepository = mockk {
      coEvery { get(book.id) } returns book
      every { flow(book.id) } returns MutableStateFlow(book)
    },
    currentBookResolver = currentBookResolver,
    player = player.apply {
      every { pauseIfCurrentBookDifferentFrom(book.id) } just Runs
    },
    sleepTimer = sleepTimer,
    playStateManager = playStateManager,
    currentBookStoreId = currentBookStoreId,
    navigator = mockk(),
    bookmarkRepository = mockk {
      coEvery { addBookmarkAtBookPosition(book, any(), any()) } returns Bookmark(
        bookId = book.id,
        chapterId = book.currentChapter.id,
        addedAt = Instant.now(),
        setBySleepTimer = true,
        id = Bookmark.Id(Uuid.random()),
        time = 0L,
        title = null,
      )
      every { bookmarksFlow(any()) } returns flowOf(emptyList())
    },
    volumeGainFormatter = mockk(),
    batteryOptimization = mockk(),
    sleepTimerPreferenceStore = sleepTimerDataStore,
    seekTimeStore = MemoryDataStore(20),
    bookId = book.id,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
    experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(false),
    kioskModeFeatureFlag = MemoryFeatureFlag(false),
    historyRecorder = historyRecorder,
    clock = clock,
  )

  @Test
  fun sleepTimerValueChanging() = scope.runTest {
    fun assertDialogSleepTime(expected: Int) {
      assertEquals(expected = BookPlayDialogViewState.SleepTimer(SleepTimerViewState(expected)), actual = viewModel.dialogState.value)
    }

    viewModel.toggleSleepTimer()
    yield()
    assertDialogSleepTime(5)

    suspend fun incrementAndAssert(time: Int) {
      viewModel.incrementSleepTime()
      yield()
      assertDialogSleepTime(time)
    }

    suspend fun decrementAndAssert(time: Int) {
      viewModel.decrementSleepTime()
      yield()
      assertDialogSleepTime(time)
    }

    decrementAndAssert(4)
    decrementAndAssert(3)
    decrementAndAssert(2)
    decrementAndAssert(1)

    decrementAndAssert(1)

    incrementAndAssert(2)
    incrementAndAssert(3)
  }

  @Test
  fun sleepTimerSettingFixedValue() = scope.runTest {
    viewModel.toggleSleepTimer()
    viewModel.onAcceptSleepTime(10)
    assertEquals(expected = 5.minutes, actual = sleepTimerDataStore.data.first().duration)
    yield()
    verify(exactly = 1) {
      sleepTimer.enable(TimedWithDuration(10.minutes))
    }
  }

  @Test
  fun deactivateSleepTimer() = scope.runTest {
    viewModel.toggleSleepTimer()
    viewModel.onAcceptSleepTime(10)
    viewModel.toggleSleepTimer()
    yield()
    verifyOrder {
      sleepTimer.enable(TimedWithDuration(10.minutes))
      sleepTimer.disable()
    }
    assertIs<SleepTimerState.Disabled>(sleepTimer.state.value)
  }

  @Test
  fun onCurrentChapterClickShowsDialogWithCorrectState() = scope.runTest {
    viewModel.onCurrentChapterClick()
    yield()

    val dialogState = assertIs<BookPlayDialogViewState.SelectChapterDialog>(viewModel.dialogState.value)

    assertEquals(
      expected = listOf(
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 1,
          name = "Chapter Start",
          active = false,
          time = "0:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 2,
          name = "Middle Section",
          active = false,
          time = "2:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 3,
          name = "Final Section",
          active = false,
          time = "4:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 4,
          name = "Chapter Start",
          active = false,
          time = "5:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 5,
          name = "Middle Section",
          active = true,
          time = "7:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 6,
          name = "Final Section",
          active = false,
          time = "9:00",
        ),
      ),
      actual = dialogState.items,
    )
  }

  @Test
  fun onChapterClickSetsPositionAndDismissesDialog() = scope.runTest {
    every { player.setPosition(any(), any(), any()) } just Runs

    viewModel.onCurrentChapterClick()
    yield()

    assertIs<BookPlayDialogViewState.SelectChapterDialog>(viewModel.dialogState.value)

    viewModel.onChapterClick(number = 2)
    yield()

    // The second mark starts at 2 minutes position in the first chapter
    verify(exactly = 1) {
      player.setPosition(
        time = 2.minutes.inWholeMilliseconds,
        id = book.chapters.first().id,
        type = ListeningEvent.Type.ChapterChange,
      )
    }

    assertEquals(expected = null, actual = viewModel.dialogState.value)
  }

  @Test
  fun `overlay prefers live controller position`() {
    val persistedBook = book()
    val overlaidBook = persistedBook.overlay(
      LivePlaybackState(
        bookId = persistedBook.id,
        chapterId = persistedBook.chapters.first().id,
        positionMs = 1.minutes.inWholeMilliseconds,
        isPlaying = true,
        playbackSpeed = 1F,
      ),
    )

    assertEquals(expected = persistedBook.chapters.first().id, actual = overlaidBook.currentChapter.id)
    assertEquals(expected = 1.minutes.inWholeMilliseconds, actual = overlaidBook.content.positionInChapter)
  }

  @Test
  fun `viewState prefers live playback state when feature flag is enabled`() = scope.runTest {
    val persistedBook = book()
    val livePlaybackFlow = MutableStateFlow<LivePlaybackState?>(null)
    val viewModel = viewModel(
      book = persistedBook,
      experimentalPlaybackPersistence = true,
      livePlaybackFlow = livePlaybackFlow,
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      assertEquals(expected = 30.seconds, actual = awaitItem()!!.playedTime)

      livePlaybackFlow.value = LivePlaybackState(
        bookId = persistedBook.id,
        chapterId = persistedBook.chapters.first().id,
        positionMs = 1.minutes.inWholeMilliseconds,
        isPlaying = true,
        playbackSpeed = 1F,
      )

      val state = awaitItem()!!
      assertEquals(expected = true, actual = state.playing)
      assertEquals(expected = "Chapter Start", actual = state.chapterName)
      assertEquals(expected = 1.minutes, actual = state.playedTime)
    }
  }

  @Test
  fun `viewState falls back to manager play state when live playback is unavailable`() = scope.runTest {
    val viewModel = viewModel(
      experimentalPlaybackPersistence = true,
      livePlaybackFlow = MutableStateFlow(null),
      playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Playing),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      val state = awaitItem()!!
      assertEquals(expected = true, actual = state.playing)
      assertEquals(expected = 30.seconds, actual = state.playedTime)
    }
  }

  @Test
  fun `viewState exposes progress through the whole book`() = scope.runTest {
    val viewModel = viewModel()

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      val state = awaitItem()!!
      assertEquals(expected = 7.5.minutes, actual = state.bookPlayedTime)
      assertEquals(expected = 10.minutes, actual = state.bookDuration)
      assertEquals(expected = 0.75F, actual = state.bookProgress)
      assertEquals(expected = 5, actual = state.chapterNumber)
      assertEquals(expected = 6, actual = state.chapterCount)
      assertEquals(expected = 6, actual = state.chapterSegments.size)
      assertEquals(expected = 1F, actual = state.chapterSegments.sum(), absoluteTolerance = 0.001F)
    }
  }

  @Test
  fun `viewState uses currently playing demo book in kiosk mode`() = scope.runTest {
    val viewModel = viewModel(kioskMode = true)

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      val state = awaitItem()!!
      assertEquals(expected = KioskModeDemoData.currentlyPlaying.title, actual = state.title)
      assertEquals(expected = KioskModeDemoData.currentlyPlaying.chapter, actual = state.chapterName)
      assertEquals(expected = KioskModeDemoData.currentlyPlaying.coverUrl, actual = state.cover)
    }
  }

  @Test
  fun `viewState pins bookmarks on the book bar in story order`() = scope.runTest {
    val second = bookmark(chapterId = book.chapters[1].id, time = 1.minutes.inWholeMilliseconds, kind = Bookmark.Kind.Favorite)
    val first = bookmark(chapterId = book.chapters[0].id, time = 2.5.minutes.inWholeMilliseconds, setBySleepTimer = true)
    val viewModel = viewModel(bookmarks = listOf(second, first))

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      var state = awaitItem()
      while (state?.bookmarkPins.isNullOrEmpty()) {
        state = awaitItem()
      }
      assertEquals(
        expected = listOf(
          BookBarPin(position = 0.25F, kind = Bookmark.Kind.Note, setBySleepTimer = true),
          BookBarPin(position = 0.6F, kind = Bookmark.Kind.Favorite, setBySleepTimer = false),
        ),
        actual = state.bookmarkPins,
      )
    }
  }

  @Test
  fun `a big jump offers the way back`() = scope.runTest {
    val player = mockk<PlayerController> {
      every { pauseIfCurrentBookDifferentFrom(book.id) } just Runs
      every { setPosition(any(), any(), any()) } just Runs
    }
    val viewModel = viewModel(player = player)
    val from = PlaybackPosition(bookId = book.id, chapterId = book.chapters[0].id, time = 2.5.minutes.inWholeMilliseconds)

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      assertNull(awaitItem()!!.jumpBack)

      historyRecorder.record(
        type = ListeningEvent.Type.ChapterChange,
        source = ListeningEvent.Source.App,
        position = from,
        to = from.copy(chapterId = book.chapters[1].id, time = 0),
      )
      val jumpBack = awaitItem()!!.jumpBack!!
      assertEquals(expected = "0:30", actual = jumpBack.time)
      assertEquals(expected = 2, actual = jumpBack.chapterNumber)
      assertEquals(expected = Duration.ZERO, actual = jumpBack.elapsed)

      viewModel.onJumpBackClick(jumpBack.id)
      verify(exactly = 1) {
        player.setPosition(time = from.time, id = from.chapterId, type = ListeningEvent.Type.JumpBack)
      }

      viewModel.onJumpBackExpire(jumpBack.id)
      assertNull(awaitItem()!!.jumpBack)
    }
  }

  @Test
  fun `an older pill expiring keeps the newer jump`() = scope.runTest {
    val viewModel = viewModel()
    val start = PlaybackPosition(bookId = book.id, chapterId = book.chapters[0].id, time = 0)

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      assertNull(awaitItem()!!.jumpBack)

      historyRecorder.record(ListeningEvent.Type.BookmarkJump, ListeningEvent.Source.App, position = start)
      val older = awaitItem()!!.jumpBack!!
      clock.advanceBy(9.seconds)
      historyRecorder.record(ListeningEvent.Type.BookmarkJump, ListeningEvent.Source.App, position = start.copy(time = 1000))
      val newer = awaitItem()!!.jumpBack!!

      viewModel.onJumpBackExpire(older.id)
      expectNoEvents()
      assertEquals(expected = newer.id, actual = historyRecorder.lastJump.value?.at?.toEpochMilli())
    }
  }

  @Test
  fun `the way back is not offered for a jump in another book`() = scope.runTest {
    val viewModel = viewModel()
    historyRecorder.record(
      type = ListeningEvent.Type.BookmarkJump,
      source = ListeningEvent.Source.App,
      position = PlaybackPosition(bookId = BookId("other"), chapterId = book.chapters[0].id, time = 0),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      assertNull(awaitItem()!!.jumpBack)
    }
  }

  private fun viewModel(
    book: Book = this.book,
    player: PlayerController? = null,
    bookmarks: List<Bookmark> = emptyList(),
    experimentalPlaybackPersistence: Boolean = false,
    kioskMode: Boolean = false,
    livePlaybackFlow: MutableStateFlow<LivePlaybackState?> = MutableStateFlow(null),
    playStateFlow: MutableStateFlow<PlayStateManager.PlayState> = MutableStateFlow(PlayStateManager.PlayState.Paused),
  ): BookPlayViewModel {
    return BookPlayViewModel(
      bookRepository = mockk {
        coEvery { get(book.id) } returns book
        every { flow(book.id) } returns MutableStateFlow(book)
      },
      currentBookResolver = currentBookResolver,
      player = (player ?: mockk()).apply {
        every { pauseIfCurrentBookDifferentFrom(book.id) } just Runs
        every { livePlaybackStateFlow(book.id) } returns livePlaybackFlow
      },
      sleepTimer = sleepTimer,
      playStateManager = mockk {
        every { this@mockk.playStateFlow } returns playStateFlow
        every { playState } returns playStateFlow.value
      },
      currentBookStoreId = MemoryDataStore(null),
      navigator = mockk(),
      bookmarkRepository = mockk {
        every { bookmarksFlow(any()) } returns flowOf(bookmarks)
      },
      volumeGainFormatter = mockk(),
      batteryOptimization = mockk(),
      sleepTimerPreferenceStore = sleepTimerDataStore,
      seekTimeStore = MemoryDataStore(20),
      bookId = book.id,
      dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
      experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(experimentalPlaybackPersistence),
      kioskModeFeatureFlag = MemoryFeatureFlag(kioskMode),
      historyRecorder = historyRecorder,
      clock = clock,
    )
  }
}

private fun bookmark(
  chapterId: ChapterId,
  time: Long,
  kind: Bookmark.Kind = Bookmark.Kind.Note,
  setBySleepTimer: Boolean = false,
): Bookmark {
  return Bookmark(
    bookId = BookId("book"),
    chapterId = chapterId,
    title = null,
    time = time,
    addedAt = Instant.EPOCH,
    setBySleepTimer = setBySleepTimer,
    id = Bookmark.Id.random(),
    kind = kind,
  )
}

private class TestClock : Clock() {

  private var instant = Instant.parse("2026-10-07T08:00:00Z")

  fun advanceBy(duration: kotlin.time.Duration) {
    instant = instant.plusMillis(duration.inWholeMilliseconds)
  }

  override fun getZone(): ZoneId = ZoneId.of("UTC")

  override fun withZone(zone: ZoneId?): Clock = this

  override fun instant(): Instant = instant
}

private fun book(
  name: String = "TestBook",
  lastPlayedAtMillis: Long = 0L,
  addedAtMillis: Long = 0L,
): Book {
  val chapters = listOf(
    chapter(),
    chapter(),
  )
  return Book(
    content = BookContent(
      author = Uuid.random().toString(),
      name = name,
      positionInChapter = 2.5.minutes.inWholeMilliseconds,
      playbackSpeed = 1F,
      addedAt = Instant.ofEpochMilli(addedAtMillis),
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = chapters[1].id,
      isActive = true,
      lastPlayedAt = Instant.ofEpochMilli(lastPlayedAtMillis),
      skipSilence = false,
      id = BookId(Uuid.random().toString()),
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
    ),
    chapters = chapters,
  )
}

private fun chapter(): Chapter {
  return Chapter(
    id = ChapterId("http://${Uuid.random()}"),
    duration = 5.minutes.inWholeMilliseconds,
    fileLastModified = Instant.EPOCH,
    markData = listOf(
      MarkData(startMs = 0L, name = "Chapter Start"),
      MarkData(startMs = 2.minutes.inWholeMilliseconds, name = "Middle Section"),
      MarkData(startMs = 4.minutes.inWholeMilliseconds, name = "Final Section"),
    ),
    name = "name",
    fileSize = 0,
  )
}

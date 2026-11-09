package voice.features.bookOverview.overview

import android.net.Uri
import androidx.datastore.core.DataStore
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Clock
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import voice.core.audiobookshelf.PickedLibrary
import voice.core.audiobookshelf.ServerDownloads
import voice.core.audiobookshelf.ServerLibrary
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.data.CustomMetadata
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.kioskmode.KioskModeDemoData
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.core.scanner.DeviceHasStoragePermissionBug
import voice.core.scanner.MediaScanTrigger
import voice.core.ui.GridCount
import voice.core.ui.GridMode
import voice.core.ui.GridModeStore
import voice.features.bookOverview.MemoryDataStore
import voice.features.bookOverview.book
import voice.features.bookOverview.hideBook.BookHider
import voice.features.bookOverview.survey.HandledSurveysStore
import voice.features.bookOverview.survey.Survey
import voice.features.bookOverview.survey.SurveyUrlFeatureFlag
import voice.navigation.Destination
import voice.navigation.Navigator

class BookOverviewViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val serverLibrary =
    mockk<ServerLibrary>(relaxUnitFun = true) {
      every { syncing } returns MutableStateFlow(false)
      every { unavailableBooks } returns flowOf(emptySet())
      every { libraries } returns flowOf(emptyList())
    }
  private val serverDownloads =
    mockk<ServerDownloads> {
      every { states } returns flowOf(emptyMap())
    }
  // combine yields, and the test dispatcher's scheduler isn't the one runTest advances
  private val dispatcherProvider =
    DispatcherProvider(
      main = testDispatcher,
      io = Dispatchers.Unconfined,
      mainImmediate = testDispatcher,
    )
  private val audiobookFolders =
    mockk<AudiobookFolders> {
      every { all() } returns flowOf(emptyMap())
      every { withoutAccess() } returns flowOf(emptyMap())
    }
  private val emptyLibraryTracker = mockk<EmptyLibraryTracker>(relaxUnitFun = true)

  @Test
  fun `state updates the current book item from live playback`() = runTest {
    val currentBook = book(name = "Current", time = 1_000)
    val otherBook = book(name = "Other", time = 2_000)
    val livePlaybackFlow = MutableStateFlow<LivePlaybackState?>(null)
    val viewModel =
      BookOverviewViewModel(
        repo =
          mockk<BookRepository> {
            every { flow() } returns MutableStateFlow(listOf(currentBook, otherBook))
          },
        mediaScanner =
          mockk<MediaScanTrigger> {
            every { scannerActive } returns MutableStateFlow(false)
            every { scan(any()) } just Runs
          },
        playStateManager = PlayStateManager(),
        playerController =
          mockk<PlayerController> {
            every { livePlaybackStateFlow(currentBook.id) } returns livePlaybackFlow
          },
        currentBookStoreDataStore = CurrentBookStore(MemoryDataStore(currentBook.id)),
        folderPickerMovedDialogShownStore =
          FolderPickerMovedDialogShownStore(MemoryDataStore(false)),
        gridModeStore = GridModeStore(MemoryDataStore(GridMode.LIST)),
        gridCount =
          mockk<GridCount> {
            every { useGridAsDefault() } returns false
          },
        navigator = mockk<Navigator>(),
        appInfoProvider = appInfoProvider(),
        deviceHasStoragePermissionBug =
          mockk<DeviceHasStoragePermissionBug> {
            every { hasBug } returns MutableStateFlow(false)
          },
        folderPickerInSettingsFeatureFlag =
          FolderPickerInSettingsFeatureFlag(MemoryFeatureFlag(false)),
        experimentalPlaybackPersistenceFeatureFlag =
          ExperimentalPlaybackPersistenceFeatureFlag(MemoryFeatureFlag(true)),
        kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(false)),
        dispatcherProvider = dispatcherProvider,
        serverLibrary = serverLibrary,
        serverDownloads = serverDownloads,
        audiobookFolders = audiobookFolders,
        emptyLibraryTracker = emptyLibraryTracker,
        survey = survey(),
        showHiddenBooksStore = ShowHiddenBooksStore(MemoryDataStore(false)),
        libraryGroupingStore = LibraryGroupingStore(MemoryDataStore(LibraryGrouping.Series)),
        showNotDownloadedBooksStore = ShowNotDownloadedBooksStore(MemoryDataStore(true)),
        hiddenSourcesStore = HiddenSourcesStore(MemoryDataStore(emptySet())),
        bookHider = BookHider(mockk()),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val initial = awaitItem()
        val initialOtherItem = initial.book(otherBook.id)

        assertEquals(expected = currentBook.toItemViewState(), actual = initial.hero?.value)
        assertEquals(expected = otherBook.toItemViewState(), actual = initialOtherItem)
        assertEquals(
          expected = listOf(otherBook.id.value),
          actual = initial.keys(BookCategory.CURRENT),
        )
        assertEquals(expected = currentBook.id, actual = initial.currentBookId)

        val livePlaybackState =
          LivePlaybackState(
            bookId = currentBook.id,
            chapterId = currentBook.chapters.first().id,
            positionMs = 6_000,
            isPlaying = true,
            playbackSpeed = 1F,
          )
        livePlaybackFlow.value = livePlaybackState
        yield()

        assertEquals(
          expected = currentBook.overlay(livePlaybackState).toItemViewState(),
          actual = initial.hero?.value,
        )
        assertEquals(expected = initialOtherItem, actual = initial.book(otherBook.id))
        expectNoEvents()
      }
  }

  @Test
  fun `state uses demo books in kiosk mode`() = runTest {
    val viewModel =
      BookOverviewViewModel(
        repo =
          mockk<BookRepository> {
            every { flow() } returns MutableStateFlow(emptyList())
          },
        mediaScanner =
          mockk<MediaScanTrigger> {
            every { scannerActive } returns MutableStateFlow(false)
            every { scan(any()) } just Runs
          },
        playStateManager = PlayStateManager(),
        playerController = mockk(),
        currentBookStoreDataStore = CurrentBookStore(MemoryDataStore(null)),
        folderPickerMovedDialogShownStore =
          FolderPickerMovedDialogShownStore(MemoryDataStore(false)),
        gridModeStore = GridModeStore(MemoryDataStore(GridMode.LIST)),
        gridCount =
          mockk<GridCount> {
            every { useGridAsDefault() } returns false
          },
        navigator = mockk<Navigator>(),
        appInfoProvider = appInfoProvider(),
        deviceHasStoragePermissionBug =
          mockk<DeviceHasStoragePermissionBug> {
            every { hasBug } returns MutableStateFlow(false)
          },
        folderPickerInSettingsFeatureFlag =
          FolderPickerInSettingsFeatureFlag(MemoryFeatureFlag(false)),
        experimentalPlaybackPersistenceFeatureFlag =
          ExperimentalPlaybackPersistenceFeatureFlag(MemoryFeatureFlag(false)),
        kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(true)),
        dispatcherProvider = dispatcherProvider,
        serverLibrary = serverLibrary,
        serverDownloads = serverDownloads,
        audiobookFolders = audiobookFolders,
        emptyLibraryTracker = emptyLibraryTracker,
        survey = survey(),
        showHiddenBooksStore = ShowHiddenBooksStore(MemoryDataStore(false)),
        libraryGroupingStore = LibraryGroupingStore(MemoryDataStore(LibraryGrouping.Series)),
        showNotDownloadedBooksStore = ShowNotDownloadedBooksStore(MemoryDataStore(true)),
        hiddenSourcesStore = HiddenSourcesStore(MemoryDataStore(emptySet())),
        bookHider = BookHider(mockk()),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        val state = awaitItem()
        assertEquals(
          expected =
            KioskModeDemoData.demoAudiobooks.map { it.id.value } -
              KioskModeDemoData.currentlyPlaying.id.value,
          actual = state.keys(BookCategory.CURRENT),
        )
        assertEquals(expected = "Echoes of Tomorrow", actual = state.hero?.value?.name)
        assertEquals(expected = KioskModeDemoData.currentlyPlaying.id, actual = state.currentBookId)
        assertEquals(expected = false, actual = state.showFolderAccessBadge)
        assertEquals(expected = false, actual = state.showFilterIcon)
      }
  }

  @Test
  fun `the books of a series are one stack, which keeps the book that is continued`() = runTest {
    val first =
      book(
        name = "First",
        series = "Saga",
        part = "1",
        time = 20_000,
        lastPlayedAt = java.time.Instant.ofEpochSecond(1),
      )
    val second =
      book(
        name = "Second",
        series = "Saga",
        part = "2",
        time = 1_000,
        lastPlayedAt = java.time.Instant.ofEpochSecond(3),
      )
    val single =
      book(name = "Single", time = 1_000, lastPlayedAt = java.time.Instant.ofEpochSecond(2))
    val viewModel = viewModel(books = listOf(first, second, single), currentBookId = second.id)

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val state = awaitItem()
        assertEquals(expected = "Second", actual = state.hero?.value?.name)
        assertEquals(expected = "Saga 2", actual = state.hero?.value?.series)
        assertEquals(
          expected = listOf("series:saga", single.id.value),
          actual = state.keys(BookCategory.CURRENT),
        )
        val stack = state.sections.getValue(BookCategory.CURRENT).first() as LibraryItem.Series
        assertEquals(expected = 2, actual = stack.series.bookCount)
        assertEquals(expected = 1, actual = stack.series.finishedCount)
        assertEquals(expected = 2, actual = state.inProgressCount)
      }
  }

  @Test
  fun `hidden books leave the library and still count for the filter`() = runTest {
    val hidden = book(name = "Hidden", time = 1_000, hidden = true)
    val visible = book(name = "Visible", time = 1_000)
    val viewModel = viewModel(books = listOf(hidden, visible), currentBookId = hidden.id)

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val state = awaitItem()
        assertEquals(
          expected = listOf(visible.id.value),
          actual = state.keys(BookCategory.CURRENT),
        )
        assertEquals(expected = null, actual = state.hero)
        assertEquals(expected = null, actual = state.playButtonState)
        assertEquals(expected = 1, actual = state.inProgressCount)
        assertEquals(expected = 1, actual = state.hiddenBookCount)
      }
  }

  @Test
  fun `a library with only hidden books isn't empty`() = runTest {
    val viewModel = viewModel(books = listOf(book(hidden = true)))

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val state = awaitItem()
        assertEquals(expected = false, actual = state.showEmptyLibrary)
        assertEquals(expected = true, actual = state.showSearchIcon)
        assertEquals(expected = emptyMap(), actual = state.sections)
      }
  }

  @Test
  fun `showing hidden books brings them back with a label`() = runTest {
    val hidden = book(name = "Hidden", time = 1_000, hidden = true)
    val store = MemoryDataStore(false)
    val viewModel = viewModel(books = listOf(hidden), showHiddenBooksStore = store)

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = emptyMap(), actual = awaitItem().sections)

        viewModel.onShowHiddenBooksChange(true)

        val state = awaitItem()
        assertEquals(expected = true, actual = state.showHiddenBooks)
        assertEquals(expected = true, actual = state.book(hidden.id).hidden)
        assertEquals(expected = true, actual = store.data.first())
      }
  }

  @Test
  fun `undo shows the hidden book again`() = runTest {
    val book = book(hidden = true)
    val repo =
      mockk<BookRepository> {
        every { flow() } returns MutableStateFlow(emptyList())
        coEvery { updateCustomMetadata(book.id, any()) } just Runs
      }
    val viewModel = viewModel(repo = repo)

    viewModel.onUndoHide(listOf(book.id))

    val update = slot<(CustomMetadata) -> CustomMetadata>()
    coVerify { repo.updateCustomMetadata(book.id, capture(update)) }
    assertEquals(expected = false, actual = update.captured(book.custom).hidden)
  }

  @Test
  fun `with hidden books shown, a stack whose books are all hidden is marked hidden`() = runTest {
    val hiddenSeries =
      listOf(
        book(series = "Saga", time = 0, hidden = true),
        book(series = "Saga", time = 0, hidden = true),
      )
    val partlyHidden =
      listOf(book(series = "Other", time = 0, hidden = true), book(series = "Other", time = 0))
    val viewModel =
      viewModel(
        books = hiddenSeries + partlyHidden,
        showHiddenBooksStore = MemoryDataStore(true),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val stacks =
          awaitItem()
            .sections
            .getValue(BookCategory.NOT_STARTED)
            .filterIsInstance<LibraryItem.Series>()
            .associate { it.series.name to it.series.hidden }
        assertEquals(expected = mapOf("Other" to false, "Saga" to true), actual = stacks)
      }
  }

  @Test
  fun `without books that aren't downloaded, the library keeps the ones on the device`() = runTest {
    val local = book(time = 0)
    val streamed = book(time = 0, id = BookId("abs://item/streamed"))
    val hiddenStreamed = book(time = 0, id = BookId("abs://item/hidden"), hidden = true)
    val downloaded = book(time = 0, id = BookId("abs://item/downloaded"))
    val downloading = book(time = 0, id = BookId("abs://item/downloading"))
    val store = MemoryDataStore(true)
    val viewModel =
      viewModel(
        books = listOf(local, streamed, hiddenStreamed, downloaded, downloading),
        downloadStates =
          flowOf(
            mapOf(
              downloaded.id to BookDownloadState.Downloaded(bytes = 2),
              downloading.id to
                BookDownloadState.Downloading(
                  progress = 0.5F,
                  downloadedBytes = 1,
                  totalBytes = 2,
                  waitingFor = null,
                ),
            ),
          ),
        showNotDownloadedBooksStore = store,
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val state = awaitItem()
        assertEquals(expected = 2, actual = state.notDownloadedBookCount)
        assertEquals(expected = 1, actual = state.hiddenBookCount)
        assertEquals(expected = 4, actual = state.keys(BookCategory.NOT_STARTED).size)

        viewModel.onShowNotDownloadedBooksChange(false)

        val filtered = awaitItem()
        assertEquals(
          expected = setOf(local.id.value, downloaded.id.value),
          actual = filtered.keys(BookCategory.NOT_STARTED).toSet(),
        )
        assertEquals(expected = 0, actual = filtered.hiddenBookCount)
        assertEquals(expected = false, actual = store.data.first())
      }
  }

  @Test
  fun `a library with server books waits for their downloads`() = runTest {
    val downloadStates = MutableStateFlow<Map<BookId, BookDownloadState>?>(null)
    val downloaded = book(time = 0, id = BookId("abs://item/downloaded"))
    val viewModel =
      viewModel(
        books = listOf(downloaded),
        downloadStates = downloadStates.filterNotNull(),
        showNotDownloadedBooksStore = MemoryDataStore(false),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())

        downloadStates.value = mapOf(downloaded.id to BookDownloadState.Downloaded(bytes = 1))

        val state = awaitItem()
        assertEquals(
          expected = listOf(downloaded.id.value),
          actual = state.keys(BookCategory.NOT_STARTED),
        )
        assertEquals(expected = false, actual = state.showNotDownloadedBooksSwitch)
      }
  }

  @Test
  fun `the current book stays when books that aren't downloaded are filtered out`() = runTest {
    val current = book(id = BookId("abs://item/current"))
    val viewModel =
      viewModel(
        books = listOf(current),
        currentBookId = current.id,
        showNotDownloadedBooksStore = MemoryDataStore(false),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val state = awaitItem()
        assertEquals(expected = current.id, actual = state.hero?.value?.id)
        assertEquals(expected = 0, actual = state.notDownloadedBookCount)
      }
  }

  @Test
  fun `a long press on a stack offers its actions`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(navigator = navigator)

    viewModel.onSeriesLongClick("Saga")

    verify {
      navigator.goTo(Destination.SeriesActions("Saga"))
    }
  }

  @Test
  fun `a stack opens its series`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(navigator = navigator)

    viewModel.onSeriesClick("Saga")

    verify {
      navigator.goTo(Destination.Series("Saga"))
    }
  }

  @Test
  fun `grouped by author, each author is one entry below current`() = runTest {
    val started = book(author = "Lena Morgan", time = 1_000)
    val notStarted = book(author = "Lena Morgan", time = 0)
    val finished = book(author = "Lena Morgan", time = 20_000)
    val noAuthor = book(author = null, time = 0)
    val store = MemoryDataStore(LibraryGrouping.Series)
    val viewModel =
      viewModel(
        books = listOf(started, notStarted, finished, noAuthor),
        libraryGroupingStore = store,
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(
          expected = listOf(notStarted.id.value, noAuthor.id.value).sorted(),
          actual = awaitItem().keys(BookCategory.NOT_STARTED).sorted(),
        )

        viewModel.onGroupingChange(LibraryGrouping.Author)

        val state = awaitItem()
        assertEquals(expected = LibraryGrouping.Author, actual = state.grouping)
        assertEquals(expected = LibraryGrouping.Author, actual = store.data.first())
        assertEquals(
          expected = listOf(started.id.value),
          actual = state.keys(BookCategory.CURRENT),
        )
        assertEquals(
          expected = listOf("author:NOT_STARTED:lenamorgan", "author:NOT_STARTED:"),
          actual = state.keys(BookCategory.NOT_STARTED),
        )
        assertEquals(
          expected = listOf("author:FINISHED:lenamorgan"),
          actual = state.keys(BookCategory.FINISHED),
        )
      }
  }

  @Test
  fun `grouped by author, the book on the continue listening card is in no author entry`() =
    runTest {
      val current = book(author = "Lena Morgan", time = 0)
      val other = book(author = "Nora Whitfield", time = 0)
      val viewModel =
        viewModel(
          books = listOf(current, other),
          currentBookId = current.id,
          libraryGroupingStore = MemoryDataStore(LibraryGrouping.Author),
        )

      backgroundScope
        .launchMolecule(RecompositionMode.Immediate) {
          viewModel.state()
        }
        .test {
          assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
          assertEquals(
            expected = listOf("author:NOT_STARTED:norawhitfield"),
            actual = awaitItem().keys(BookCategory.NOT_STARTED),
          )
        }
    }

  @Test
  fun `an author opens their screen`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(navigator = navigator)

    viewModel.onAuthorClick("Lena Morgan")
    viewModel.onAuthorClick(null)

    verify {
      navigator.goTo(Destination.Author("Lena Morgan"))
      navigator.goTo(Destination.Author(null))
    }
  }

  @Test
  fun `folder picker icon is hidden when folder picker in settings flag is true`() = runTest {
    val viewModel =
      viewModel(
        folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(true),
        folderPickerMovedDialogShownStore = MemoryDataStore(false),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = false, actual = awaitItem().showFolderPickerIcon)
      }
  }

  @Test
  fun `folder picker icon is shown once when flag is false`() = runTest {
    val viewModel =
      viewModel(
        folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
        folderPickerMovedDialogShownStore = MemoryDataStore(false),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = true, actual = awaitItem().showFolderPickerIcon)
      }
  }

  @Test
  fun `folder picker icon is hidden when moved dialog was shown`() = runTest {
    val viewModel =
      viewModel(
        folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
        folderPickerMovedDialogShownStore = MemoryDataStore(true),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = false, actual = awaitItem().showFolderPickerIcon)
      }
  }

  @Test
  fun `folder picker icon is hidden for installs on migration cutoff date`() = runTest {
    val viewModel =
      viewModel(
        folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
        folderPickerMovedDialogShownStore = MemoryDataStore(false),
        appInfoProvider = appInfoProvider(installTime = Instant.parse("2026-06-17T00:00:00Z")),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = false, actual = awaitItem().showFolderPickerIcon)
      }
  }

  @Test
  fun `folder picker click shows moved dialog instead of navigating`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel =
      viewModel(
        navigator = navigator,
        folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
        folderPickerMovedDialogShownStore = MemoryDataStore(false),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = null, actual = awaitItem().dialog)

        viewModel.onBookFolderClick()

        assertEquals(
          expected = BookOverviewViewState.Dialog.FolderPickerMovedToSettings,
          actual = awaitItem().dialog,
        )
        verify(exactly = 0) {
          navigator.goTo(Destination.FolderPicker)
        }
      }
  }

  @Test
  fun `dismissing moved dialog marks it shown and hides folder picker icon`() = runTest {
    val folderPickerMovedDialogShownStore = MemoryDataStore(false)
    val viewModel =
      viewModel(
        folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
        folderPickerMovedDialogShownStore = folderPickerMovedDialogShownStore,
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = true, actual = awaitItem().showFolderPickerIcon)

        viewModel.onBookFolderClick()
        assertEquals(
          expected = BookOverviewViewState.Dialog.FolderPickerMovedToSettings,
          actual = awaitItem().dialog,
        )

        viewModel.onFolderPickerMovedDialogDismiss()

        val dismissed = awaitItem()
        assertEquals(expected = null, actual = dismissed.dialog)
        if (dismissed.showFolderPickerIcon) {
          assertEquals(expected = false, actual = awaitItem().showFolderPickerIcon)
        } else {
          assertEquals(expected = false, actual = dismissed.showFolderPickerIcon)
        }
      }
  }

  @Test
  fun `current book id is dropped when the book is no longer in the library`() = runTest {
    val book = book(name = "Present", time = 1_000)
    val viewModel = viewModel(books = listOf(book), currentBookId = BookId("removed"))

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = null, actual = awaitItem().currentBookId)
      }
  }

  @Test
  fun `a library without books shows that no books were found`() = runTest {
    val viewModel = viewModel()

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = true, actual = awaitItem().showEmptyLibrary)
      }
  }

  @Test
  fun `an empty library shows that no books were found only once scanning is done`() = runTest {
    val scanning = MutableStateFlow(true)
    val viewModel = viewModel(scanning = scanning)

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        testScheduler.runCurrent()
        assertEquals(expected = false, actual = expectMostRecentItem().showEmptyLibrary)
        scanning.value = false
        assertEquals(expected = true, actual = awaitItem().showEmptyLibrary)
      }
  }

  @Test
  fun `an empty library is tracked`() = runTest {
    val viewModel = viewModel()

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = true, actual = awaitItem().showEmptyLibrary)
        testScheduler.runCurrent()
      }

    coVerify { emptyLibraryTracker.track() }
  }

  @Test
  fun `a hidden source leaves the library`() = runTest {
    every { serverLibrary.libraries } returns
      flowOf(listOf(PickedLibrary("mine", "Mine"), PickedLibrary("kids", "Kids")))
    val mine = serverBook(libraryId = "mine")
    val kids = serverBook(libraryId = "kids")
    val hiddenKids = serverBook(libraryId = "kids", hidden = true)
    val kidsSource = LibrarySource.ServerLibrary("kids")
    val viewModel =
      viewModel(
        books = listOf(mine, kids, hiddenKids),
        currentBookId = kids.id,
        hiddenSourcesStore = MemoryDataStore(setOf(kidsSource)),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val state = awaitItem()
        assertEquals(expected = listOf(mine.id.value), actual = state.keys(BookCategory.CURRENT))
        assertEquals(expected = null, actual = state.hero)
        assertEquals(expected = 0, actual = state.hiddenBookCount)
        assertEquals(
          expected =
            listOf(
              LibrarySourceViewState(
                LibrarySource.ServerLibrary("mine"),
                name = "Mine",
                bookCount = 1,
                shown = true,
              ),
              LibrarySourceViewState(kidsSource, name = "Kids", bookCount = 2, shown = false),
            ),
          actual = state.sources,
        )
      }
  }

  @Test
  fun `a single source has no switch`() = runTest {
    every { serverLibrary.libraries } returns flowOf(listOf(PickedLibrary("mine", "Mine")))
    val viewModel = viewModel(books = listOf(serverBook(libraryId = "mine")))

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = emptyList(), actual = awaitItem().sources)
      }
  }

  @Test
  fun `with every source hidden, all books show`() = runTest {
    every { serverLibrary.libraries } returns
      flowOf(listOf(PickedLibrary("mine", "Mine"), PickedLibrary("kids", "Kids")))
    val mine = serverBook(libraryId = "mine")
    val kids = serverBook(libraryId = "kids")
    val viewModel =
      viewModel(
        books = listOf(mine, kids),
        hiddenSourcesStore =
          MemoryDataStore(
            setOf(LibrarySource.ServerLibrary("mine"), LibrarySource.ServerLibrary("kids")),
          ),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        val state = awaitItem()
        assertEquals(
          expected = setOf(mine.id.value, kids.id.value),
          actual = state.keys(BookCategory.CURRENT).toSet(),
        )
        assertEquals(expected = listOf(true, true), actual = state.sources.map { it.shown })
      }
  }

  @Test
  fun `switching a source hides and shows it`() = runTest {
    every { serverLibrary.libraries } returns
      flowOf(listOf(PickedLibrary("mine", "Mine"), PickedLibrary("kids", "Kids")))
    val kids = LibrarySource.ServerLibrary("kids")
    val store = MemoryDataStore<Set<LibrarySource>>(emptySet())
    val viewModel = viewModel(hiddenSourcesStore = store)

    viewModel.onSourceChange(kids, show = false)
    assertEquals(expected = setOf(kids), actual = store.data.first())

    viewModel.onSourceChange(kids, show = true)
    assertEquals(expected = emptySet(), actual = store.data.first())
  }

  @Test
  fun `with every source hidden, switching one off hides only that one`() = runTest {
    every { serverLibrary.libraries } returns
      flowOf(listOf(PickedLibrary("mine", "Mine"), PickedLibrary("kids", "Kids")))
    val mine = LibrarySource.ServerLibrary("mine")
    val kids = LibrarySource.ServerLibrary("kids")
    val removed = LibrarySource.Folder("content://removed")
    val store = MemoryDataStore<Set<LibrarySource>>(setOf(mine, kids, removed))
    val viewModel = viewModel(hiddenSourcesStore = store)

    viewModel.onSourceChange(kids, show = false)

    assertEquals(expected = setOf(kids, removed), actual = store.data.first())
  }

  @Test
  fun `folders without access show the badge`() = runTest {
    every { audiobookFolders.withoutAccess() } returns
      flowOf(mapOf(FolderType.Root to listOf(mockk<Uri>())))
    val viewModel = viewModel()

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
        assertEquals(expected = true, actual = awaitItem().showFolderAccessBadge)
      }
  }

  @Test
  fun `a survey shows its card`() = runTest {
    val viewModel = viewModel(books = listOf(book()), survey = survey(remoteUrl = SURVEY_URL))

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        testScheduler.runCurrent()
        assertEquals(expected = true, actual = expectMostRecentItem().showSurveyCard)
      }
  }

  @Test
  fun `an empty library hides the survey card`() = runTest {
    val viewModel = viewModel(survey = survey(remoteUrl = SURVEY_URL))

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        testScheduler.runCurrent()
        val state = expectMostRecentItem()
        assertEquals(expected = true, actual = state.showEmptyLibrary)
        assertEquals(expected = false, actual = state.showSurveyCard)
      }
  }

  @Test
  fun `taking the survey opens it with the app info and hides the card`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel =
      viewModel(
        books = listOf(book()),
        navigator = navigator,
        survey = survey(remoteUrl = SURVEY_URL),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        testScheduler.runCurrent()
        assertEquals(expected = true, actual = expectMostRecentItem().showSurveyCard)

        viewModel.onSurveyClick()
        testScheduler.runCurrent()

        assertEquals(expected = false, actual = expectMostRecentItem().showSurveyCard)
      }
    verify {
      navigator.goTo(
        Destination.Website(
          "$SURVEY_URL?app_version=1.2.3&source=org.fdroid.fdroid&days_installed=14",
        ),
      )
    }
  }

  @Test
  fun `dismissing the survey hides the card`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel =
      viewModel(
        books = listOf(book()),
        navigator = navigator,
        survey = survey(remoteUrl = SURVEY_URL),
      )

    backgroundScope
      .launchMolecule(RecompositionMode.Immediate) {
        viewModel.state()
      }
      .test {
        testScheduler.runCurrent()
        assertEquals(expected = true, actual = expectMostRecentItem().showSurveyCard)

        viewModel.onSurveyDismiss()
        testScheduler.runCurrent()

        assertEquals(expected = false, actual = expectMostRecentItem().showSurveyCard)
      }
    verify(exactly = 0) {
      navigator.goTo(any())
    }
  }

  @Test
  fun `show folders opens the folders`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(navigator = navigator)

    viewModel.onShowFoldersClick()

    verify {
      navigator.goTo(Destination.FolderPicker)
    }
  }

  @Test
  fun `search click opens the search`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(navigator = navigator)

    viewModel.onSearchClick()

    verify {
      navigator.goTo(Destination.LibrarySearch)
    }
  }

  private fun BookOverviewViewState.book(bookId: BookId): BookOverviewItemViewState {
    return sections.values
      .flatten()
      .filterIsInstance<LibraryItem.Book>()
      .single { it.id == bookId }
      .state
      .value
  }

  private fun serverBook(
    libraryId: String,
    hidden: Boolean = false,
  ): Book {
    val book = book(id = BookId("abs://item/${Uuid.random()}"), hidden = hidden)
    return book.copy(content = book.content.copy(serverLibraryId = libraryId))
  }

  private fun BookOverviewViewState.keys(category: BookCategory): List<String> {
    return sections.getValue(category).map { it.key }
  }

  private fun viewModel(
    folderPickerInSettingsFeatureFlag: MemoryFeatureFlag<Boolean> = MemoryFeatureFlag(false),
    folderPickerMovedDialogShownStore: DataStore<Boolean> = MemoryDataStore(false),
    navigator: Navigator = mockk(),
    appInfoProvider: AppInfoProvider = appInfoProvider(),
    books: List<Book> = emptyList(),
    currentBookId: BookId? = null,
    scanning: Flow<Boolean> = MutableStateFlow(false),
    survey: Survey = survey(),
    showHiddenBooksStore: DataStore<Boolean> = MemoryDataStore(false),
    libraryGroupingStore: DataStore<LibraryGrouping> = MemoryDataStore(LibraryGrouping.Series),
    showNotDownloadedBooksStore: DataStore<Boolean> = MemoryDataStore(true),
    downloadStates: Flow<Map<BookId, BookDownloadState>> = flowOf(emptyMap()),
    hiddenSourcesStore: DataStore<Set<LibrarySource>> = MemoryDataStore(emptySet()),
    repo: BookRepository = mockk {
      every { flow() } returns MutableStateFlow(books)
    },
  ): BookOverviewViewModel {
    return BookOverviewViewModel(
      repo = repo,
      mediaScanner =
        mockk<MediaScanTrigger> {
          every { scannerActive } returns scanning
          every { scan(any()) } just Runs
        },
      playStateManager = PlayStateManager(),
      playerController = mockk(),
      currentBookStoreDataStore = CurrentBookStore(MemoryDataStore(currentBookId)),
      folderPickerMovedDialogShownStore =
        FolderPickerMovedDialogShownStore(folderPickerMovedDialogShownStore),
      gridModeStore = GridModeStore(MemoryDataStore(GridMode.LIST)),
      gridCount =
        mockk<GridCount> {
          every { useGridAsDefault() } returns false
        },
      navigator = navigator,
      appInfoProvider = appInfoProvider,
      deviceHasStoragePermissionBug =
        mockk<DeviceHasStoragePermissionBug> {
          every { hasBug } returns MutableStateFlow(false)
        },
      folderPickerInSettingsFeatureFlag =
        FolderPickerInSettingsFeatureFlag(folderPickerInSettingsFeatureFlag),
      experimentalPlaybackPersistenceFeatureFlag =
        ExperimentalPlaybackPersistenceFeatureFlag(MemoryFeatureFlag(false)),
      kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(false)),
      dispatcherProvider = dispatcherProvider,
      serverLibrary = serverLibrary,
      serverDownloads =
        mockk {
          every { states } returns downloadStates
        },
      audiobookFolders = audiobookFolders,
      emptyLibraryTracker = emptyLibraryTracker,
      survey = survey,
      showHiddenBooksStore = ShowHiddenBooksStore(showHiddenBooksStore),
      libraryGroupingStore = LibraryGroupingStore(libraryGroupingStore),
      showNotDownloadedBooksStore = ShowNotDownloadedBooksStore(showNotDownloadedBooksStore),
      hiddenSourcesStore = HiddenSourcesStore(hiddenSourcesStore),
      bookHider = BookHider(repo),
    )
  }

  private fun survey(remoteUrl: String = ""): Survey {
    return Survey(
      surveyUrlFeatureFlag = SurveyUrlFeatureFlag(MemoryFeatureFlag(remoteUrl)),
      handledSurveysStore = HandledSurveysStore(MemoryDataStore(emptyMap())),
      localSurvey =
        mockk {
          coEvery { url() } returns null
        },
      clock = Clock.fixed(java.time.Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC),
      appInfoProvider =
        mockk {
          every { versionName } returns "1.2.3"
          every { installer } returns "org.fdroid.fdroid"
          every { installTime } returns Instant.parse("2026-10-06T10:00:00Z")
        },
      analytics = mockk(relaxUnitFun = true),
    )
  }

  private fun appInfoProvider(
    installTime: Instant = Instant.parse("2026-06-16T00:00:00Z"),
  ): AppInfoProvider {
    return mockk {
      every { this@mockk.installTime } returns installTime
    }
  }

  private companion object {
    const val SURVEY_URL = "https://survey.example/s"
  }
}

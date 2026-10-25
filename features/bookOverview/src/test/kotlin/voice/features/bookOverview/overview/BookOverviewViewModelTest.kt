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
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import voice.core.audiobookshelf.ServerLibrary
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
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
import voice.features.bookOverview.survey.HandledSurveysStore
import voice.features.bookOverview.survey.Survey
import voice.features.bookOverview.survey.SurveyUrlFeatureFlag
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.Clock
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BookOverviewViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private val serverLibrary = mockk<ServerLibrary>(relaxUnitFun = true) {
    every { syncing } returns flowOf(false)
    every { unavailableBooks } returns flowOf(emptySet())
    every { downloadStates } returns flowOf(emptyMap())
  }
  private val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
  private val audiobookFolders = mockk<AudiobookFolders> {
    every { withoutAccess() } returns flowOf(emptyMap())
  }
  private val emptyLibraryTracker = mockk<EmptyLibraryTracker>(relaxUnitFun = true)

  @Test
  fun `state updates the current book item from live playback`() = runTest {
    val currentBook = book(name = "Current", time = 1_000)
    val otherBook = book(name = "Other", time = 2_000)
    val livePlaybackFlow = MutableStateFlow<LivePlaybackState?>(null)
    val viewModel = BookOverviewViewModel(
      repo = mockk<BookRepository> {
        every { flow() } returns MutableStateFlow(listOf(currentBook, otherBook))
      },
      mediaScanner = mockk<MediaScanTrigger> {
        every { scannerActive } returns MutableStateFlow(false)
        every { scan(any()) } just Runs
      },
      playStateManager = PlayStateManager(),
      playerController = mockk<PlayerController> {
        every { livePlaybackStateFlow(currentBook.id) } returns livePlaybackFlow
      },
      currentBookStoreDataStore = CurrentBookStore(MemoryDataStore(currentBook.id)),
      folderPickerMovedDialogShownStore = FolderPickerMovedDialogShownStore(MemoryDataStore(false)),
      gridModeStore = GridModeStore(MemoryDataStore(GridMode.LIST)),
      gridCount = mockk<GridCount> {
        every { useGridAsDefault() } returns false
      },
      navigator = mockk<Navigator>(),
      appInfoProvider = appInfoProvider(),
      deviceHasStoragePermissionBug = mockk<DeviceHasStoragePermissionBug> {
        every { hasBug } returns MutableStateFlow(false)
      },
      folderPickerInSettingsFeatureFlag = FolderPickerInSettingsFeatureFlag(MemoryFeatureFlag(false)),
      experimentalPlaybackPersistenceFeatureFlag = ExperimentalPlaybackPersistenceFeatureFlag(MemoryFeatureFlag(true)),
      kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(false)),
      dispatcherProvider = dispatcherProvider,
      serverLibrary = serverLibrary,
      audiobookFolders = audiobookFolders,
      emptyLibraryTracker = emptyLibraryTracker,
      survey = survey(),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      val initial = awaitItem()
      val initialCurrentItem = initial.currentBook(currentBook.id)
      val initialOtherItem = initial.currentBook(otherBook.id)
      val initialKeys = initial.books.getValue(BookOverviewCategory.CURRENT).keys.toList()

      assertEquals(expected = currentBook.toItemViewState(), actual = initialCurrentItem)
      assertEquals(expected = otherBook.toItemViewState(), actual = initialOtherItem)
      assertEquals(expected = currentBook.id, actual = initial.currentBookId)

      val livePlaybackState = LivePlaybackState(
        bookId = currentBook.id,
        chapterId = currentBook.chapters.first().id,
        positionMs = 6_000,
        isPlaying = true,
        playbackSpeed = 1F,
      )
      livePlaybackFlow.value = livePlaybackState
      yield()

      assertEquals(expected = initialKeys, actual = initial.books.getValue(BookOverviewCategory.CURRENT).keys.toList())
      assertEquals(expected = currentBook.overlay(livePlaybackState).toItemViewState(), actual = initial.currentBook(currentBook.id))
      assertEquals(expected = initialOtherItem, actual = initial.currentBook(otherBook.id))
      expectNoEvents()
    }
  }

  @Test
  fun `state uses demo books in kiosk mode`() = runTest {
    val viewModel = BookOverviewViewModel(
      repo = mockk<BookRepository> {
        every { flow() } returns MutableStateFlow(emptyList())
      },
      mediaScanner = mockk<MediaScanTrigger> {
        every { scannerActive } returns MutableStateFlow(false)
        every { scan(any()) } just Runs
      },
      playStateManager = PlayStateManager(),
      playerController = mockk(),
      currentBookStoreDataStore = CurrentBookStore(MemoryDataStore(null)),
      folderPickerMovedDialogShownStore = FolderPickerMovedDialogShownStore(MemoryDataStore(false)),
      gridModeStore = GridModeStore(MemoryDataStore(GridMode.LIST)),
      gridCount = mockk<GridCount> {
        every { useGridAsDefault() } returns false
      },
      navigator = mockk<Navigator>(),
      appInfoProvider = appInfoProvider(),
      deviceHasStoragePermissionBug = mockk<DeviceHasStoragePermissionBug> {
        every { hasBug } returns MutableStateFlow(false)
      },
      folderPickerInSettingsFeatureFlag = FolderPickerInSettingsFeatureFlag(MemoryFeatureFlag(false)),
      experimentalPlaybackPersistenceFeatureFlag = ExperimentalPlaybackPersistenceFeatureFlag(MemoryFeatureFlag(false)),
      kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(true)),
      dispatcherProvider = dispatcherProvider,
      serverLibrary = serverLibrary,
      audiobookFolders = audiobookFolders,
      emptyLibraryTracker = emptyLibraryTracker,
      survey = survey(),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      val state = awaitItem()
      assertEquals(
        expected = KioskModeDemoData.demoAudiobooks.map {
          it.id
        },
        actual = state.books.getValue(BookOverviewCategory.CURRENT).keys.toList(),
      )
      assertEquals(expected = "Echoes of Tomorrow", actual = state.currentBook(KioskModeDemoData.currentlyPlaying.id).name)
      assertEquals(expected = KioskModeDemoData.currentlyPlaying.id, actual = state.currentBookId)
      assertEquals(expected = false, actual = state.showFolderAccessBadge)
    }
  }

  @Test
  fun `folder picker icon is hidden when folder picker in settings flag is true`() = runTest {
    val viewModel = viewModel(
      folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(true),
      folderPickerMovedDialogShownStore = MemoryDataStore(false),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = false, actual = awaitItem().showFolderPickerIcon)
    }
  }

  @Test
  fun `folder picker icon is shown once when flag is false`() = runTest {
    val viewModel = viewModel(
      folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
      folderPickerMovedDialogShownStore = MemoryDataStore(false),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = true, actual = awaitItem().showFolderPickerIcon)
    }
  }

  @Test
  fun `folder picker icon is hidden when moved dialog was shown`() = runTest {
    val viewModel = viewModel(
      folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
      folderPickerMovedDialogShownStore = MemoryDataStore(true),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = false, actual = awaitItem().showFolderPickerIcon)
    }
  }

  @Test
  fun `folder picker icon is hidden for installs on migration cutoff date`() = runTest {
    val viewModel = viewModel(
      folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
      folderPickerMovedDialogShownStore = MemoryDataStore(false),
      appInfoProvider = appInfoProvider(installTime = Instant.parse("2026-06-17T00:00:00Z")),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = false, actual = awaitItem().showFolderPickerIcon)
    }
  }

  @Test
  fun `folder picker click shows moved dialog instead of navigating`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(
      navigator = navigator,
      folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
      folderPickerMovedDialogShownStore = MemoryDataStore(false),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = null, actual = awaitItem().dialog)

      viewModel.onBookFolderClick()

      assertEquals(expected = BookOverviewViewState.Dialog.FolderPickerMovedToSettings, actual = awaitItem().dialog)
      verify(exactly = 0) {
        navigator.goTo(Destination.FolderPicker)
      }
    }
  }

  @Test
  fun `dismissing moved dialog marks it shown and hides folder picker icon`() = runTest {
    val folderPickerMovedDialogShownStore = MemoryDataStore(false)
    val viewModel = viewModel(
      folderPickerInSettingsFeatureFlag = MemoryFeatureFlag(false),
      folderPickerMovedDialogShownStore = folderPickerMovedDialogShownStore,
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = true, actual = awaitItem().showFolderPickerIcon)

      viewModel.onBookFolderClick()
      assertEquals(expected = BookOverviewViewState.Dialog.FolderPickerMovedToSettings, actual = awaitItem().dialog)

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

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = null, actual = awaitItem().currentBookId)
    }
  }

  @Test
  fun `a library without books shows that no books were found`() = runTest {
    val viewModel = viewModel()

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = true, actual = awaitItem().showEmptyLibrary)
    }
  }

  @Test
  fun `an empty library shows that no books were found only once scanning is done`() = runTest {
    val scanning = MutableStateFlow(true)
    val viewModel = viewModel(scanning = scanning)

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      testScheduler.runCurrent()
      assertEquals(expected = false, actual = expectMostRecentItem().showEmptyLibrary)
      scanning.value = false
      assertEquals(expected = true, actual = awaitItem().showEmptyLibrary)
    }
  }

  @Test
  fun `an empty library is tracked`() = runTest {
    val viewModel = viewModel()

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = true, actual = awaitItem().showEmptyLibrary)
      testScheduler.runCurrent()
    }

    coVerify { emptyLibraryTracker.track() }
  }

  @Test
  fun `folders without access show the badge`() = runTest {
    every { audiobookFolders.withoutAccess() } returns flowOf(mapOf(FolderType.Root to listOf(mockk<Uri>())))
    val viewModel = viewModel()

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      assertEquals(expected = BookOverviewViewState.Loading, actual = awaitItem())
      assertEquals(expected = true, actual = awaitItem().showFolderAccessBadge)
    }
  }

  @Test
  fun `a survey shows its card`() = runTest {
    val viewModel = viewModel(books = listOf(book()), survey = survey(remoteUrl = SURVEY_URL))

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      testScheduler.runCurrent()
      assertEquals(expected = true, actual = expectMostRecentItem().showSurveyCard)
    }
  }

  @Test
  fun `an empty library hides the survey card`() = runTest {
    val viewModel = viewModel(survey = survey(remoteUrl = SURVEY_URL))

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      testScheduler.runCurrent()
      val state = expectMostRecentItem()
      assertEquals(expected = true, actual = state.showEmptyLibrary)
      assertEquals(expected = false, actual = state.showSurveyCard)
    }
  }

  @Test
  fun `taking the survey opens it with the app info and hides the card`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(books = listOf(book()), navigator = navigator, survey = survey(remoteUrl = SURVEY_URL))

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
      testScheduler.runCurrent()
      assertEquals(expected = true, actual = expectMostRecentItem().showSurveyCard)

      viewModel.onSurveyClick()
      testScheduler.runCurrent()

      assertEquals(expected = false, actual = expectMostRecentItem().showSurveyCard)
    }
    verify {
      navigator.goTo(Destination.Website("$SURVEY_URL?app_version=1.2.3&source=org.fdroid.fdroid&days_installed=14"))
    }
  }

  @Test
  fun `dismissing the survey hides the card`() = runTest {
    val navigator = mockk<Navigator>(relaxed = true)
    val viewModel = viewModel(books = listOf(book()), navigator = navigator, survey = survey(remoteUrl = SURVEY_URL))

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.state()
    }.test {
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

  private fun BookOverviewViewState.currentBook(bookId: BookId): BookOverviewItemViewState {
    return books.getValue(BookOverviewCategory.CURRENT).getValue(bookId).value
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
  ): BookOverviewViewModel {
    return BookOverviewViewModel(
      repo = mockk<BookRepository> {
        every { flow() } returns MutableStateFlow(books)
      },
      mediaScanner = mockk<MediaScanTrigger> {
        every { scannerActive } returns scanning
        every { scan(any()) } just Runs
      },
      playStateManager = PlayStateManager(),
      playerController = mockk(),
      currentBookStoreDataStore = CurrentBookStore(MemoryDataStore(currentBookId)),
      folderPickerMovedDialogShownStore = FolderPickerMovedDialogShownStore(folderPickerMovedDialogShownStore),
      gridModeStore = GridModeStore(MemoryDataStore(GridMode.LIST)),
      gridCount = mockk<GridCount> {
        every { useGridAsDefault() } returns false
      },
      navigator = navigator,
      appInfoProvider = appInfoProvider,
      deviceHasStoragePermissionBug = mockk<DeviceHasStoragePermissionBug> {
        every { hasBug } returns MutableStateFlow(false)
      },
      folderPickerInSettingsFeatureFlag = FolderPickerInSettingsFeatureFlag(folderPickerInSettingsFeatureFlag),
      experimentalPlaybackPersistenceFeatureFlag = ExperimentalPlaybackPersistenceFeatureFlag(MemoryFeatureFlag(false)),
      kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(false)),
      dispatcherProvider = dispatcherProvider,
      serverLibrary = serverLibrary,
      audiobookFolders = audiobookFolders,
      emptyLibraryTracker = emptyLibraryTracker,
      survey = survey,
    )
  }

  private fun survey(remoteUrl: String = ""): Survey {
    return Survey(
      surveyUrlFeatureFlag = SurveyUrlFeatureFlag(MemoryFeatureFlag(remoteUrl)),
      handledSurveysStore = HandledSurveysStore(MemoryDataStore(emptyMap())),
      localSurvey = mockk {
        coEvery { url() } returns null
      },
      clock = Clock.fixed(java.time.Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC),
      appInfoProvider = mockk {
        every { versionName } returns "1.2.3"
        every { installer } returns "org.fdroid.fdroid"
        every { installTime } returns Instant.parse("2026-10-06T10:00:00Z")
      },
      analytics = mockk(relaxUnitFun = true),
    )
  }

  private fun appInfoProvider(installTime: Instant = Instant.parse("2026-06-16T00:00:00Z")): AppInfoProvider {
    return mockk {
      every { this@mockk.installTime } returns installTime
    }
  }

  private companion object {
    const val SURVEY_URL = "https://survey.example/s"
  }
}

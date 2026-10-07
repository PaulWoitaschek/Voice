package voice.features.settings

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.data.GridMode
import voice.core.data.KioskModeDemoData
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.DocumentFileWithUri
import voice.core.data.folders.FolderType
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.documentfile.CachedDocumentFile
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.history.ListeningHistoryRecorder
import voice.core.ui.DynamicColorAvailability
import voice.core.ui.GridCount
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SettingsViewModelTest {

  private val scope = TestScope()
  private val themeModeStore = MemoryDataStore(ThemeMode.FollowSystem)
  private val themeColorSchemeStore = MemoryDataStore(ThemeColorScheme.VoiceBlue)
  private val autoRewindAmountStore = MemoryDataStore(10)
  private val seekTimeStore = MemoryDataStore(30)
  private val gridModeStore = MemoryDataStore(GridMode.GRID)
  private val sleepTimerPreferenceStore = MemoryDataStore(SleepTimerPreference.Default)
  private val analyticsConsentStore = MemoryDataStore(false)
  private val developerMenuUnlockedStore = MemoryDataStore(false)
  private val listeningHistoryEnabledStore = MemoryDataStore(true)
  private val listeningHistoryRepo = mockk<ListeningHistoryRepo> {
    coEvery { clear() } just Runs
  }
  private val listeningHistoryRecorder = ListeningHistoryRecorder(
    repo = listeningHistoryRepo,
    clock = Clock.systemUTC(),
    scope = scope,
  )
  private val navigator = mockk<Navigator> {
    every { goTo(any()) } just Runs
  }
  private val appInfoProvider = mockk<AppInfoProvider> {
    every { versionName } returns "1.2.3"
    every { analyticsIncluded } returns true
    every { supportDevelopmentIncluded } returns true
    every { installTime } returns Instant.parse("2026-06-01T00:00:00Z")
  }
  private val gridCount = mockk<GridCount> {
    every { useGridAsDefault() } returns true
  }
  private val kioskModeFeatureFlag = MemoryFeatureFlag(false)
  private val dynamicColorAvailability = mockk<DynamicColorAvailability> {
    every { isSupported() } returns true
  }
  private val audiobookFolders = mockk<AudiobookFolders> {
    every { all() } returns flowOf(emptyMap())
  }

  private val viewModel = SettingsViewModel(
    themeModeStore = themeModeStore,
    themeColorSchemeStore = themeColorSchemeStore,
    autoRewindAmountStore = autoRewindAmountStore,
    seekTimeStore = seekTimeStore,
    navigator = navigator,
    appInfoProvider = appInfoProvider,
    gridModeStore = gridModeStore,
    sleepTimerPreferenceStore = sleepTimerPreferenceStore,
    analyticsConsentStore = analyticsConsentStore,
    gridCount = gridCount,
    kioskModeFeatureFlag = kioskModeFeatureFlag,
    developerMenuUnlockedStore = developerMenuUnlockedStore,
    dynamicColorAvailability = dynamicColorAvailability,
    audiobookFolders = audiobookFolders,
    listeningHistoryEnabledStore = listeningHistoryEnabledStore,
    listeningHistoryRecorder = listeningHistoryRecorder,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun `view state defaults to follow system and voice blue`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      awaitItem().let {
        assertEquals(expected = ThemeMode.FollowSystem, actual = it.themeMode)
        assertEquals(expected = ThemeColorScheme.VoiceBlue, actual = it.themeColorScheme)
      }
    }
  }

  @Test
  fun `theme mode changes update view state`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      assertEquals(expected = ThemeMode.FollowSystem, actual = awaitItem().themeMode)

      viewModel.setThemeMode(ThemeMode.Dark)
      assertEquals(expected = ThemeMode.Dark, actual = awaitItem().themeMode)

      viewModel.setThemeMode(ThemeMode.Light)
      assertEquals(expected = ThemeMode.Light, actual = awaitItem().themeMode)

      viewModel.setThemeMode(ThemeMode.FollowSystem)
      assertEquals(expected = ThemeMode.FollowSystem, actual = awaitItem().themeMode)
    }
  }

  @Test
  fun `dynamic color is offered when supported`() = scope.runTest {
    every { dynamicColorAvailability.isSupported() } returns true

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      assertEquals(expected = true, actual = awaitItem().dynamicColorAvailable)
    }
  }

  @Test
  fun `dynamic color is not offered when unsupported`() = scope.runTest {
    every { dynamicColorAvailability.isSupported() } returns false

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      assertEquals(expected = false, actual = awaitItem().dynamicColorAvailable)
    }
  }

  @Test
  fun `selecting dynamic color updates view state`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      assertEquals(expected = ThemeColorScheme.VoiceBlue, actual = awaitItem().themeColorScheme)

      viewModel.setThemeColorScheme(ThemeColorScheme.Dynamic)

      assertEquals(expected = ThemeColorScheme.Dynamic, actual = awaitItem().themeColorScheme)
    }
  }

  @Test
  fun `developer menu is hidden until app version tapped 13 times`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      assertEquals(expected = false, actual = awaitItem().showDeveloperMenu)

      repeat(13) {
        viewModel.onAppVersionClick()
      }

      assertEquals(expected = true, actual = awaitItem().showDeveloperMenu)
    }
  }

  @Test
  fun `developer menu unlock emits snackbar effect`() = scope.runTest {
    viewModel.viewEffects.test {
      repeat(13) {
        viewModel.onAppVersionClick()
      }

      assertIs<SettingsViewEffect.DeveloperMenuUnlocked>(awaitItem())
    }
  }

  @Test
  fun `openDeveloperMenu navigates to developer settings`() {
    viewModel.openDeveloperMenu()

    verify(exactly = 1) {
      navigator.goTo(Destination.DeveloperSettings)
    }
  }

  @Test
  fun `openSupportVoice navigates to support screen`() {
    viewModel.openSupportVoice()

    verify(exactly = 1) {
      navigator.goTo(Destination.SupportVoice)
    }
  }

  @Test
  fun `openFolderPicker navigates to folder picker`() {
    viewModel.openFolderPicker()

    verify(exactly = 1) {
      navigator.goTo(Destination.FolderPicker)
    }
  }

  @Test
  fun `view state shows support development when included`() = scope.runTest {
    every { appInfoProvider.supportDevelopmentIncluded } returns true

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      assertEquals(expected = true, actual = awaitItem().showSupportDevelopment)
    }
  }

  @Test
  fun `view state hides support development when not included`() = scope.runTest {
    every { appInfoProvider.supportDevelopmentIncluded } returns false

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      assertEquals(expected = false, actual = awaitItem().showSupportDevelopment)
    }
  }

  @Test
  fun `listening history can be turned off and on`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      assertEquals(expected = true, actual = awaitItem().listeningHistoryEnabled)

      viewModel.toggleListeningHistory()
      assertEquals(expected = false, actual = awaitItem().listeningHistoryEnabled)

      viewModel.toggleListeningHistory()
      assertEquals(expected = true, actual = awaitItem().listeningHistoryEnabled)
    }
  }

  @Test
  fun `view state lists the audiobook folder names alphabetically`() = scope.runTest {
    every { audiobookFolders.all() } returns flowOf(
      mapOf(
        FolderType.Root to listOf(folder("Sci-Fi")),
        FolderType.SingleFolder to listOf(folder("crime"), folder("Audiobooks")),
      ),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      runCurrent()
      assertEquals(expected = listOf("Audiobooks", "crime", "Sci-Fi"), actual = expectMostRecentItem().folderNames)
    }
  }

  @Test
  fun `clearing the listening history tells so`() = scope.runTest {
    viewModel.viewEffects.test {
      viewModel.clearListeningHistory()
      assertEquals(expected = SettingsViewEffect.ListeningHistoryCleared, actual = awaitItem())
    }
    coVerify(exactly = 1) { listeningHistoryRepo.clear() }
  }

  @Test
  fun `kiosk mode shows sample folder names`() = scope.runTest {
    kioskModeFeatureFlag.value = true

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      runCurrent()
      assertEquals(expected = KioskModeDemoData.folderNames, actual = expectMostRecentItem().folderNames)
    }
  }

  @Test
  fun `setUseGrid stores the layout`() = scope.runTest {
    viewModel.setUseGrid(false)
    runCurrent()
    assertEquals(expected = GridMode.LIST, actual = gridModeStore.data.first())

    viewModel.setUseGrid(true)
    runCurrent()
    assertEquals(expected = GridMode.GRID, actual = gridModeStore.data.first())
  }

  @Test
  fun `follow device layout resolves through the grid count`() = scope.runTest {
    gridModeStore.updateData { GridMode.FOLLOW_DEVICE }
    every { gridCount.useGridAsDefault() } returns false

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.filterNotNull().test {
      runCurrent()
      assertEquals(expected = false, actual = expectMostRecentItem().useGrid)
    }
  }

  @Test
  fun `skip amount stays within its range`() = scope.runTest {
    viewModel.seekAmountChanged(15)
    runCurrent()
    assertEquals(expected = 15, actual = seekTimeStore.data.first())

    viewModel.seekAmountChanged(1)
    runCurrent()
    assertEquals(expected = 3, actual = seekTimeStore.data.first())

    viewModel.seekAmountChanged(90)
    runCurrent()
    assertEquals(expected = 60, actual = seekTimeStore.data.first())
  }

  @Test
  fun `view state is loading until every setting is read`() = scope.runTest {
    every { audiobookFolders.all() } returns flow {
      delay(1.seconds)
      emit(emptyMap())
    }

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      runCurrent()
      assertNull(expectMostRecentItem())

      advanceTimeBy(1.seconds)
      runCurrent()
      assertNotNull(expectMostRecentItem())
    }
  }

  @Test
  fun `quick steps of the skip amount add up and stay within its range`() = scope.runTest {
    viewModel.seekAmountStepped(1)
    viewModel.seekAmountStepped(1)
    runCurrent()
    assertEquals(expected = 32, actual = seekTimeStore.data.first())

    seekTimeStore.updateData { 59 }
    viewModel.seekAmountStepped(1)
    viewModel.seekAmountStepped(1)
    runCurrent()
    assertEquals(expected = 60, actual = seekTimeStore.data.first())
  }

  @Test
  fun `quick steps of auto rewind add up and stay within its range`() = scope.runTest {
    viewModel.autoRewindAmountStepped(-1)
    viewModel.autoRewindAmountStepped(-1)
    runCurrent()
    assertEquals(expected = 8, actual = autoRewindAmountStore.data.first())

    autoRewindAmountStore.updateData { 1 }
    viewModel.autoRewindAmountStepped(-1)
    viewModel.autoRewindAmountStepped(-1)
    runCurrent()
    assertEquals(expected = 0, actual = autoRewindAmountStore.data.first())
  }

  @Test
  fun `auto rewind stays within its range`() = scope.runTest {
    viewModel.autoRewindAmountChanged(0)
    runCurrent()
    assertEquals(expected = 0, actual = autoRewindAmountStore.data.first())

    viewModel.autoRewindAmountChanged(-1)
    runCurrent()
    assertEquals(expected = 0, actual = autoRewindAmountStore.data.first())

    viewModel.autoRewindAmountChanged(25)
    runCurrent()
    assertEquals(expected = 20, actual = autoRewindAmountStore.data.first())
  }
}

private fun folder(name: String) = DocumentFileWithUri(
  documentFile = mockk<CachedDocumentFile> {
    every { this@mockk.name } returns name
    every { isFile } returns false
  },
  uri = mockk(),
)

private class MemoryDataStore<T>(initial: T) : DataStore<T> {

  private val value = MutableStateFlow(initial)

  override val data: Flow<T> get() = value

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    return value.updateAndGet { transform(it) }
  }
}

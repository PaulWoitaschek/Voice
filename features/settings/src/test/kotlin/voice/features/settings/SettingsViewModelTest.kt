package voice.features.settings

import androidx.datastore.core.DataStore
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.data.GridMode
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.ui.GridCount
import voice.navigation.Destination
import voice.navigation.Navigator

class SettingsViewModelTest {

  private val scope = TestScope()
  private val useDarkThemeStore = MemoryDataStore(false)
  private val autoRewindAmountStore = MemoryDataStore(10)
  private val seekTimeStore = MemoryDataStore(30)
  private val gridModeStore = MemoryDataStore(GridMode.GRID)
  private val sleepTimerPreferenceStore = MemoryDataStore(SleepTimerPreference.Default)
  private val analyticsConsentStore = MemoryDataStore(false)
  private val developerMenuUnlockedStore = MemoryDataStore(false)
  private val navigator = mockk<Navigator> {
    every { goTo(any()) } just Runs
  }
  private val appInfoProvider = mockk<AppInfoProvider> {
    every { versionName } returns "1.2.3"
    every { analyticsIncluded } returns true
  }
  private val gridCount = mockk<GridCount> {
    every { useGridAsDefault() } returns true
  }
  private val folderPickerFeatureFlag = MemoryFeatureFlag(false)

  private val viewModel = SettingsViewModel(
    useDarkThemeStore = useDarkThemeStore,
    autoRewindAmountStore = autoRewindAmountStore,
    seekTimeStore = seekTimeStore,
    navigator = navigator,
    appInfoProvider = appInfoProvider,
    gridModeStore = gridModeStore,
    sleepTimerPreferenceStore = sleepTimerPreferenceStore,
    analyticsConsentStore = analyticsConsentStore,
    gridCount = gridCount,
    folderPickerInSettingsFeatureFlag = folderPickerFeatureFlag,
    developerMenuUnlockedStore = developerMenuUnlockedStore,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun `developer menu is hidden until app version tapped 13 times`() = scope.runTest {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      awaitItem().showDeveloperMenu shouldBe false

      repeat(13) {
        viewModel.onAppVersionClick()
      }

      awaitItem().showDeveloperMenu shouldBe true
    }
  }

  @Test
  fun `openDeveloperMenu navigates to developer settings`() {
    viewModel.openDeveloperMenu()

    verify(exactly = 1) {
      navigator.goTo(Destination.DeveloperSettings)
    }
  }
}

private class MemoryDataStore<T>(initial: T) : DataStore<T> {

  private val value = MutableStateFlow(initial)

  override val data: Flow<T> get() = value

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    return value.updateAndGet { transform(it) }
  }
}

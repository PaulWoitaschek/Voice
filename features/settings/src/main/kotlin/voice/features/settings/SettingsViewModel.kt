package voice.features.settings

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.GridMode
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import voice.core.data.folders.AudiobookFolders
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.data.store.AnalyticsConsentStore
import voice.core.data.store.AutoRewindAmountStore
import voice.core.data.store.DeveloperMenuUnlockedStore
import voice.core.data.store.GridModeStore
import voice.core.data.store.SeekTimeStore
import voice.core.data.store.SleepTimerPreferenceStore
import voice.core.data.store.ThemeColorSchemeStore
import voice.core.data.store.ThemeModeStore
import voice.core.documentfile.nameWithoutExtension
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.KioskModeFeatureFlagQualifier
import voice.core.ui.DynamicColorAvailability
import voice.core.ui.GridCount
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.LocalTime

@Inject
class SettingsViewModel(
  @ThemeModeStore
  private val themeModeStore: DataStore<ThemeMode>,
  @ThemeColorSchemeStore
  private val themeColorSchemeStore: DataStore<ThemeColorScheme>,
  @AutoRewindAmountStore
  private val autoRewindAmountStore: DataStore<Int>,
  @SeekTimeStore
  private val seekTimeStore: DataStore<Int>,
  private val navigator: Navigator,
  private val appInfoProvider: AppInfoProvider,
  @GridModeStore
  private val gridModeStore: DataStore<GridMode>,
  @SleepTimerPreferenceStore
  private val sleepTimerPreferenceStore: DataStore<SleepTimerPreference>,
  @AnalyticsConsentStore
  private val analyticsConsentStore: DataStore<Boolean>,
  private val gridCount: GridCount,
  @KioskModeFeatureFlagQualifier
  private val kioskModeFeatureFlag: FeatureFlag<Boolean>,
  @DeveloperMenuUnlockedStore
  private val developerMenuUnlockedStore: DataStore<Boolean>,
  private val dynamicColorAvailability: DynamicColorAvailability,
  private val audiobookFolders: AudiobookFolders,
  private val dispatcherProvider: DispatcherProvider,
) : SettingsListener {

  private val mainScope = MainScope(dispatcherProvider)
  internal val viewEffects: SharedFlow<SettingsViewEffect>
    field = MutableSharedFlow<SettingsViewEffect>(extraBufferCapacity = 1)
  private var appVersionTapCount = 0

  @Composable
  fun viewState(): SettingsViewState {
    val themeMode by remember { themeModeStore.data }.collectAsState(initial = ThemeMode.FollowSystem)
    val themeColorScheme by remember { themeColorSchemeStore.data }.collectAsState(initial = ThemeColorScheme.VoiceBlue)
    val autoRewindAmount by remember { autoRewindAmountStore.data }.collectAsState(initial = 0)
    val seekTime by remember { seekTimeStore.data }.collectAsState(initial = 0)
    val gridMode by remember { gridModeStore.data }.collectAsState(initial = GridMode.GRID)
    val autoSleepTimer by remember { sleepTimerPreferenceStore.data }.collectAsState(
      initial = SleepTimerPreference.Default,
    )
    val analyticsEnabled by remember { analyticsConsentStore.data }.collectAsState(initial = false)
    val folderNames by remember { folderNames() }.collectAsState(initial = emptyList())
    val showDeveloperMenu by remember { developerMenuUnlockedStore.data }.collectAsState(initial = false)
    val dynamicColorAvailable = remember {
      dynamicColorAvailability.isSupported()
    }
    return SettingsViewState(
      themeMode = themeMode,
      themeColorScheme = themeColorScheme,
      dynamicColorAvailable = dynamicColorAvailable,
      seekTimeInSeconds = seekTime,
      autoRewindInSeconds = autoRewindAmount,
      appVersion = appInfoProvider.versionName,
      useGrid = when (gridMode) {
        GridMode.LIST -> false
        GridMode.GRID -> true
        GridMode.FOLLOW_DEVICE -> gridCount.useGridAsDefault()
      },
      autoSleepTimer = SettingsViewState.AutoSleepTimerViewState(
        enabled = autoSleepTimer.autoSleepTimerEnabled,
        startTime = autoSleepTimer.autoSleepStartTime,
        endTime = autoSleepTimer.autoSleepEndTime,
      ),
      analyticsEnabled = analyticsEnabled,
      showAnalyticSetting = appInfoProvider.analyticsIncluded,
      showDeveloperMenu = showDeveloperMenu,
      showSupportDevelopment = appInfoProvider.supportDevelopmentIncluded,
      folderNames = folderNames,
    )
  }

  private fun folderNames(): Flow<List<String>> {
    if (kioskModeFeatureFlag.get()) {
      return flowOf(listOf("Audiobooks", "Sci-Fi", "Non-Fiction"))
    }
    return audiobookFolders.all().map { folders ->
      withContext(dispatcherProvider.io) {
        folders.values.flatten()
          .map { it.documentFile.nameWithoutExtension() }
          .sortedWith(String.CASE_INSENSITIVE_ORDER)
      }
    }
  }

  override fun close() {
    navigator.goBack()
  }

  override fun setThemeMode(themeMode: ThemeMode) {
    mainScope.launch {
      themeModeStore.updateData { themeMode }
    }
  }

  override fun setThemeColorScheme(themeColorScheme: ThemeColorScheme) {
    mainScope.launch {
      themeColorSchemeStore.updateData { themeColorScheme }
    }
  }

  override fun setUseGrid(useGrid: Boolean) {
    mainScope.launch {
      gridModeStore.updateData {
        if (useGrid) GridMode.GRID else GridMode.LIST
      }
    }
  }

  override fun seekAmountChanged(seconds: Int) {
    mainScope.launch {
      seekTimeStore.updateData { seconds.coerceIn(SEEK_TIME_RANGE) }
    }
  }

  override fun autoRewindAmountChanged(seconds: Int) {
    mainScope.launch {
      autoRewindAmountStore.updateData { seconds.coerceIn(AUTO_REWIND_RANGE) }
    }
  }

  override fun getSupport() {
    navigator.goTo(Destination.Website("https://github.com/PaulWoitaschek/Voice/discussions/categories/q-a"))
  }

  override fun suggestIdea() {
    navigator.goTo(Destination.Website("https://github.com/PaulWoitaschek/Voice/discussions/categories/ideas"))
  }

  override fun openBugReport() {
    val url = "https://github.com/PaulWoitaschek/Voice/issues/new".toUri()
      .buildUpon()
      .appendQueryParameter("template", "bug.yml")
      .appendQueryParameter("version", appInfoProvider.versionName)
      .appendQueryParameter("androidversion", Build.VERSION.SDK_INT.toString())
      .appendQueryParameter("device", Build.MODEL)
      .toString()
    navigator.goTo(Destination.Website(url))
  }

  override fun openTranslations() {
    navigator.goTo(Destination.Website("https://hosted.weblate.org/engage/voice/"))
  }

  override fun openFaq() {
    navigator.goTo(Destination.Website("https://voice.woitaschek.de/faq/"))
  }

  override fun openSupportVoice() {
    navigator.goTo(Destination.SupportVoice)
  }

  override fun openFolderPicker() {
    navigator.goTo(Destination.FolderPicker)
  }

  override fun setAutoSleepTimer(checked: Boolean) {
    mainScope.launch {
      sleepTimerPreferenceStore.updateData { currentPrefs ->
        currentPrefs.copy(autoSleepTimerEnabled = checked)
      }
    }
  }

  override fun setAutoSleepTimerStart(time: LocalTime) {
    mainScope.launch {
      sleepTimerPreferenceStore.updateData { currentPrefs ->
        currentPrefs.copy(autoSleepStartTime = time)
      }
    }
  }

  override fun setAutoSleepTimerEnd(time: LocalTime) {
    mainScope.launch {
      sleepTimerPreferenceStore.updateData { currentPrefs ->
        currentPrefs.copy(autoSleepEndTime = time)
      }
    }
  }

  override fun toggleAnalytics() {
    mainScope.launch {
      analyticsConsentStore.updateData { !it }
    }
  }

  override fun onAppVersionClick() {
    mainScope.launch {
      if (developerMenuUnlockedStore.data.first()) {
        return@launch
      }
      if (++appVersionTapCount >= 13) {
        developerMenuUnlockedStore.updateData { true }
        viewEffects.emit(SettingsViewEffect.DeveloperMenuUnlocked)
      }
    }
  }

  override fun openDeveloperMenu() {
    navigator.goTo(Destination.DeveloperSettings)
  }
}

internal val SEEK_TIME_RANGE = 3..60
internal val AUTO_REWIND_RANGE = 0..20

package voice.features.settings

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.GridMode
import voice.core.data.KioskModeDemoData
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import voice.core.data.folders.AudiobookFolders
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.data.store.AnalyticsConsentStore
import voice.core.data.store.AutoRewindAmountStore
import voice.core.data.store.DeveloperMenuUnlockedStore
import voice.core.data.store.GridModeStore
import voice.core.data.store.ListeningHistoryEnabledStore
import voice.core.data.store.SeekTimeStore
import voice.core.data.store.SleepTimerPreferenceStore
import voice.core.data.store.ThemeColorSchemeStore
import voice.core.data.store.ThemeModeStore
import voice.core.documentfile.nameWithoutExtension
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.KioskModeFeatureFlagQualifier
import voice.core.playback.history.ListeningHistoryRecorder
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
  @ListeningHistoryEnabledStore
  private val listeningHistoryEnabledStore: DataStore<Boolean>,
  private val listeningHistoryRecorder: ListeningHistoryRecorder,
  private val dispatcherProvider: DispatcherProvider,
) : SettingsListener {

  private val mainScope = MainScope(dispatcherProvider)
  internal val viewEffects: SharedFlow<SettingsViewEffect>
    field = MutableSharedFlow<SettingsViewEffect>(extraBufferCapacity = 1)
  private var appVersionTapCount = 0

  /**
   * Null until every setting is read. Placeholder values would show for a moment and then animate
   * over to the real ones, as if they had just been changed.
   */
  @Composable
  fun viewState(): SettingsViewState? {
    val themeMode = remember { themeModeStore.data }.collectAsState(initial = null).value
    val themeColorScheme = remember { themeColorSchemeStore.data }.collectAsState(initial = null).value
    val autoRewindAmount = remember { autoRewindAmountStore.data }.collectAsState(initial = null).value
    val seekTime = remember { seekTimeStore.data }.collectAsState(initial = null).value
    val gridMode = remember { gridModeStore.data }.collectAsState(initial = null).value
    val autoSleepTimer = remember { sleepTimerPreferenceStore.data }.collectAsState(initial = null).value
    val analyticsEnabled = remember { analyticsConsentStore.data }.collectAsState(initial = null).value
    val listeningHistoryEnabled = remember { listeningHistoryEnabledStore.data }.collectAsState(initial = null).value
    // reading the folders and their names asks other processes, so that's kept off the main thread
    val folderNames = remember { folderNames() }.collectAsState(initial = null, context = dispatcherProvider.io).value
    val showDeveloperMenu = remember { developerMenuUnlockedStore.data }.collectAsState(initial = null).value
    val dynamicColorAvailable = remember {
      dynamicColorAvailability.isSupported()
    }
    if (
      themeMode == null ||
      themeColorScheme == null ||
      autoRewindAmount == null ||
      seekTime == null ||
      gridMode == null ||
      autoSleepTimer == null ||
      analyticsEnabled == null ||
      listeningHistoryEnabled == null ||
      folderNames == null ||
      showDeveloperMenu == null
    ) {
      return null
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
      listeningHistoryEnabled = listeningHistoryEnabled,
    )
  }

  private fun folderNames(): Flow<List<String>> {
    if (kioskModeFeatureFlag.get()) {
      return flowOf(KioskModeDemoData.folderNames)
    }
    return audiobookFolders.all().map { folders ->
      folders.values.flatten()
        .map { it.documentFile.nameWithoutExtension() }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
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

  override fun seekAmountStepped(step: Int) {
    mainScope.launch {
      // relative to the stored amount, so quick taps add up before the screen catches up
      seekTimeStore.updateData { (it + step).coerceIn(SEEK_TIME_RANGE) }
    }
  }

  override fun autoRewindAmountChanged(seconds: Int) {
    mainScope.launch {
      autoRewindAmountStore.updateData { seconds.coerceIn(AUTO_REWIND_RANGE) }
    }
  }

  override fun autoRewindAmountStepped(step: Int) {
    mainScope.launch {
      autoRewindAmountStore.updateData { (it + step).coerceIn(AUTO_REWIND_RANGE) }
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

  override fun toggleListeningHistory() {
    mainScope.launch {
      listeningHistoryEnabledStore.updateData { !it }
    }
  }

  override fun clearListeningHistory() {
    mainScope.launch {
      listeningHistoryRecorder.clear()
      viewEffects.emit(SettingsViewEffect.ListeningHistoryCleared)
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

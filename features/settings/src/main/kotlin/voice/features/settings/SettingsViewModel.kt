package voice.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.analytics.api.AnalyticsConsentStore
import voice.core.audiobookshelf.AudiobookshelfFeatureFlag
import voice.core.audiobookshelf.ServerLibrary
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.common.FeedbackLinks
import voice.core.common.MainScope
import voice.core.data.folders.AudiobookFolders
import voice.core.data.supporter.SupporterStatus
import voice.core.data.supporter.SupporterStatusStore
import voice.core.documentfile.nameWithoutExtension
import voice.core.featureflag.FeatureFlag
import voice.core.kioskmode.KioskModeDemoData
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.playback.AutoRewindAmountStore
import voice.core.playback.LockScreenSliderStore
import voice.core.playback.SeekTimeStore
import voice.core.sleeptimer.SleepTimerPreference
import voice.core.sleeptimer.SleepTimerPreferenceStore
import voice.core.ui.CoverColorsStore
import voice.core.ui.DynamicColorAvailability
import voice.core.ui.GridCount
import voice.core.ui.GridMode
import voice.core.ui.GridModeStore
import voice.core.ui.PureBlackStore
import voice.core.ui.ThemeColorScheme
import voice.core.ui.ThemeColorSchemeStore
import voice.core.ui.ThemeMode
import voice.core.ui.ThemeModeStore
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth

@Inject
class SettingsViewModel(
  private val themeModeStore: ThemeModeStore,
  private val themeColorSchemeStore: ThemeColorSchemeStore,
  private val pureBlackStore: PureBlackStore,
  private val coverColorsStore: CoverColorsStore,
  private val autoRewindAmountStore: AutoRewindAmountStore,
  private val seekTimeStore: SeekTimeStore,
  private val lockScreenSliderStore: LockScreenSliderStore,
  private val navigator: Navigator,
  private val appInfoProvider: AppInfoProvider,
  private val gridModeStore: GridModeStore,
  private val sleepTimerPreferenceStore: SleepTimerPreferenceStore,
  private val analyticsConsentStore: AnalyticsConsentStore,
  private val gridCount: GridCount,
  private val kioskModeFeatureFlag: KioskModeFeatureFlag,
  private val developerMenuUnlockedStore: DeveloperMenuUnlockedStore,
  private val dynamicColorAvailability: DynamicColorAvailability,
  private val audiobookFolders: AudiobookFolders,
  private val supportDevelopmentFeatureFlag: SupportDevelopmentFeatureFlag,
  private val supporterStatusStore: SupporterStatusStore,
  private val clock: Clock,
  private val dispatcherProvider: DispatcherProvider,
  private val serverLibrary: ServerLibrary,
  private val audiobookshelfFeatureFlag: AudiobookshelfFeatureFlag,
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
    val pureBlack = remember { pureBlackStore.data }.collectAsState(initial = null).value
    val coverColors = remember { coverColorsStore.data }.collectAsState(initial = null).value
    val autoRewindAmount = remember { autoRewindAmountStore.data }.collectAsState(initial = null).value
    val seekTime = remember { seekTimeStore.data }.collectAsState(initial = null).value
    val lockScreenSlider = remember { lockScreenSliderStore.data }.collectAsState(initial = null).value
    val gridMode = remember { gridModeStore.data }.collectAsState(initial = null).value
    val autoSleepTimer = remember { sleepTimerPreferenceStore.data }.collectAsState(initial = null).value
    val analyticsEnabled = remember { analyticsConsentStore.data }.collectAsState(initial = null).value
    // reading the folders and their names asks other processes, so that's kept off the main thread
    val folderNames = remember { folderNames() }.collectAsState(initial = null, context = dispatcherProvider.io).value
    val foldersWithoutAccess = remember { foldersWithoutAccess() }.collectAsState(initial = null, context = dispatcherProvider.io).value
    val showDeveloperMenu = remember { developerMenuUnlockedStore.data }.collectAsState(initial = null).value
    val showSupportDevelopment = remember { supportDevelopmentFeatureFlag.flow.map { it.value } }.collectAsState(initial = null).value
    val supporterStatus = remember { supporterStatusStore.data }.collectAsState(initial = null).value
    val audiobookshelfServer = remember {
      serverLibrary.serverName.map(::AudiobookshelfServerName)
    }.collectAsState(initial = null).value
    val audiobookshelfEnabled = remember { audiobookshelfFeatureFlag.flow.map { it.value } }
      .collectAsState(initial = null).value
    val dynamicColorAvailable = remember {
      dynamicColorAvailability.isSupported()
    }
    if (
      themeMode == null ||
      themeColorScheme == null ||
      pureBlack == null ||
      coverColors == null ||
      autoRewindAmount == null ||
      seekTime == null ||
      lockScreenSlider == null ||
      gridMode == null ||
      autoSleepTimer == null ||
      analyticsEnabled == null ||
      folderNames == null ||
      foldersWithoutAccess == null ||
      showDeveloperMenu == null ||
      showSupportDevelopment == null ||
      supporterStatus == null ||
      audiobookshelfServer == null ||
      audiobookshelfEnabled == null
    ) {
      return null
    }
    return SettingsViewState(
      themeMode = themeMode,
      themeColorScheme = themeColorScheme,
      pureBlack = pureBlack,
      coverColors = coverColors,
      dynamicColorAvailable = dynamicColorAvailable,
      seekTimeInSeconds = seekTime,
      autoRewindInSeconds = autoRewindAmount,
      lockScreenSlider = lockScreenSlider,
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
      showSupportDevelopment = showSupportDevelopment,
      supporterBadge = supporterStatus.badge,
      supporterSince = supporterStatus.supporterSince?.let {
        YearMonth.from(Instant.ofEpochMilli(it).atZone(clock.zone))
      },
      folderNames = folderNames,
      foldersWithoutAccess = foldersWithoutAccess,
      audiobookshelfServer = audiobookshelfServer.name,
      // a connected server stays reachable, so it can be signed out of
      showAudiobookshelf = audiobookshelfEnabled || audiobookshelfServer.name != null,
    )
  }

  /** Tells a server that is still loading apart from no server at all. */
  private data class AudiobookshelfServerName(val name: String?)

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

  private fun foldersWithoutAccess(): Flow<Boolean> {
    if (kioskModeFeatureFlag.get()) {
      return flowOf(false)
    }
    return audiobookFolders.withoutAccess().map { folders -> folders.values.any { it.isNotEmpty() } }
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

  override fun togglePureBlack() {
    mainScope.launch {
      pureBlackStore.updateData { !it }
    }
  }

  override fun toggleCoverColors() {
    mainScope.launch {
      coverColorsStore.updateData { !it }
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

  override fun toggleLockScreenSlider() {
    mainScope.launch {
      lockScreenSliderStore.updateData { !it }
    }
  }

  override fun getSupport() {
    navigator.goTo(Destination.Website(FeedbackLinks.QUESTIONS))
  }

  override fun suggestIdea() {
    navigator.goTo(Destination.Website(FeedbackLinks.IDEAS))
  }

  override fun openBugReport() {
    navigator.goTo(Destination.Website(FeedbackLinks.bugReport(appInfoProvider.versionName)))
  }

  override fun openFaq() {
    navigator.goTo(Destination.Website(FeedbackLinks.FAQ))
  }

  override fun openSupportVoice() {
    navigator.goTo(Destination.SupportVoice)
  }

  override fun openFolderPicker() {
    navigator.goTo(Destination.FolderPicker)
  }

  override fun openAudiobookshelf() {
    mainScope.launch {
      navigator.goTo(
        if (serverLibrary.isConnected()) {
          Destination.AudiobookshelfSettings
        } else {
          Destination.AudiobookshelfLogin(Origin.Default)
        },
      )
    }
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

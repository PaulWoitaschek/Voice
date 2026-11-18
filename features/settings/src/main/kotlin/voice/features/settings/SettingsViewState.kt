package voice.features.settings

import java.time.LocalTime
import java.time.YearMonth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import voice.core.data.supporter.SupporterBadge
import voice.core.ui.ThemeColorScheme
import voice.core.ui.ThemeMode

data class SettingsViewState(
  val themeMode: ThemeMode,
  val themeColorScheme: ThemeColorScheme,
  val pureBlack: Boolean,
  val coverColors: Boolean,
  val dynamicColorAvailable: Boolean,
  val seekTime: Duration,
  val autoRewind: Duration,
  val lockScreenSlider: Boolean,
  val appVersion: String,
  val useGrid: Boolean,
  val autoSleepTimer: AutoSleepTimerViewState,
  val showAnalyticSetting: Boolean,
  val analyticsEnabled: Boolean,
  val showDeveloperMenu: Boolean,
  val supporterBadge: SupporterBadge?,
  val supporterSince: YearMonth?,
  val folderNames: List<String>,
  val foldersWithoutAccess: Boolean,
  /** The connected Audiobookshelf server, or null without one. */
  val audiobookshelfServer: String?,
  val showAudiobookshelf: Boolean,
) {

  companion object {
    fun preview(): SettingsViewState {
      return SettingsViewState(
        themeMode = ThemeMode.FollowSystem,
        themeColorScheme = ThemeColorScheme.VoiceBlue,
        pureBlack = false,
        coverColors = true,
        dynamicColorAvailable = true,
        seekTime = 20.seconds,
        autoRewind = 2.seconds,
        lockScreenSlider = true,
        appVersion = "1.2.3",
        useGrid = true,
        autoSleepTimer = AutoSleepTimerViewState.preview(),
        analyticsEnabled = false,
        showAnalyticSetting = true,
        showDeveloperMenu = true,
        supporterBadge = null,
        supporterSince = null,
        folderNames = listOf("Audiobooks", "Sci-Fi", "Non-Fiction"),
        foldersWithoutAccess = false,
        audiobookshelfServer = "audiobooks.example.com",
        showAudiobookshelf = true,
      )
    }
  }

  data class AutoSleepTimerViewState(
    val enabled: Boolean,
    val startTime: LocalTime,
    val endTime: LocalTime,
  ) {
    companion object {
      fun preview(): AutoSleepTimerViewState {
        return AutoSleepTimerViewState(
          enabled = false,
          startTime = LocalTime.of(22, 0),
          endTime = LocalTime.of(6, 0),
        )
      }
    }
  }
}

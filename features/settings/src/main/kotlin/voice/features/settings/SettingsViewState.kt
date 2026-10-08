package voice.features.settings

import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import voice.core.data.supporter.SupporterBadge
import java.time.LocalTime
import java.time.YearMonth

data class SettingsViewState(
  val themeMode: ThemeMode,
  val themeColorScheme: ThemeColorScheme,
  val dynamicColorAvailable: Boolean,
  val seekTimeInSeconds: Int,
  val autoRewindInSeconds: Int,
  val appVersion: String,
  val useGrid: Boolean,
  val autoSleepTimer: AutoSleepTimerViewState,
  val showAnalyticSetting: Boolean,
  val analyticsEnabled: Boolean,
  val showDeveloperMenu: Boolean,
  val showSupportDevelopment: Boolean,
  /** The badge of someone who supports Voice, shown on the support card as a thank-you. */
  val supporterBadge: SupporterBadge?,
  val supporterSince: YearMonth?,
  val folderNames: List<String>,
  val listeningHistoryEnabled: Boolean,
) {

  companion object {
    fun preview(): SettingsViewState {
      return SettingsViewState(
        themeMode = ThemeMode.FollowSystem,
        themeColorScheme = ThemeColorScheme.VoiceBlue,
        dynamicColorAvailable = true,
        seekTimeInSeconds = 20,
        autoRewindInSeconds = 2,
        appVersion = "1.2.3",
        useGrid = true,
        autoSleepTimer = AutoSleepTimerViewState.preview(),
        analyticsEnabled = false,
        showAnalyticSetting = true,
        showDeveloperMenu = true,
        showSupportDevelopment = true,
        supporterBadge = null,
        supporterSince = null,
        folderNames = listOf("Audiobooks", "Sci-Fi", "Non-Fiction"),
        listeningHistoryEnabled = true,
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

package voice.features.settings

import java.time.LocalTime

data class SettingsViewState(
  val useDarkTheme: Boolean,
  val showDarkThemePref: Boolean,
  val seekTimeInSeconds: Int,
  val autoRewindInSeconds: Int,
  val appVersion: String,
  val dialog: Dialog?,
  val useGrid: Boolean,
  val autoSleepTimer: AutoSleepTimerViewState,
  val showAnalyticSetting: Boolean,
  val analyticsEnabled: Boolean,
) {

  enum class Dialog {
    AutoRewindAmount,
    SeekTime,
  }

  companion object {
    fun preview(): SettingsViewState {
      return SettingsViewState(
        useDarkTheme = false,
        showDarkThemePref = true,
        seekTimeInSeconds = 42,
        autoRewindInSeconds = 12,
        dialog = null,
        appVersion = "1.2.3",
        useGrid = true,
        autoSleepTimer = AutoSleepTimerViewState.preview(),
        analyticsEnabled = false,
        showAnalyticSetting = true,
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

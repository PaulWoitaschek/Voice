package voice.settings

import voice.common.autoSleepTimer.AutoSleepTimer

data class SettingsViewState(
  val useDarkTheme: Boolean,
  val showDarkThemePref: Boolean,
  val seekTimeInSeconds: Int,
  val autoRewindInSeconds: Int,
  val appVersion: String,
  val dialog: Dialog?,
  val useGrid: Boolean,
  val autoSleepTimer: AutoSleepTimer,
) {

  enum class Dialog {
    AutoRewindAmount,
    SeekTime,
  }
}

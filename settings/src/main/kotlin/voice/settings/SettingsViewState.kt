package voice.settings

import voice.pref.AutoSleepTimerPrefs

data class SettingsViewState(
  val useDarkTheme: Boolean,
  val showDarkThemePref: Boolean,
  val seekTimeInSeconds: Int,
  val autoRewindInSeconds: Int,
  val appVersion: String,
  val dialog: Dialog?,
  val useGrid: Boolean,
  val autoSleepTimer: AutoSleepTimerPrefs,
) {

  enum class Dialog {
    AutoRewindAmount,
    SeekTime,
  }
}

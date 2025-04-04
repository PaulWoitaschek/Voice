package voice.settings

data class SettingsViewState(
  val useDarkTheme: Boolean,
  val showDarkThemePref: Boolean,
  val seekTimeInSeconds: Int,
  val autoRewindInSeconds: Int,
  val appVersion: String,
  val dialog: Dialog?,
  val useGrid: Boolean,
  val autoSleepTimer: Boolean,
  val autoSleepTimeStart: String,
  val autoSleepTimeEnd: String,
) {

  enum class Dialog {
    AutoRewindAmount,
    SeekTime,
  }
}

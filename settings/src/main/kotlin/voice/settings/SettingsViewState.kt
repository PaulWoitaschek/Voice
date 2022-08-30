package voice.settings

data class SettingsViewState(
  val useDarkTheme: Boolean,
  val showDarkThemePref: Boolean,
  val resumeOnReplug: Boolean,
  val seekTimeInSeconds: Int,
  val autoRewindInSeconds: Int,
  val appVersion: String,
  val dialog: Dialog?,
) {

  enum class Dialog {
    Contribute, AutoRewindAmount, SeekTime
  }
}

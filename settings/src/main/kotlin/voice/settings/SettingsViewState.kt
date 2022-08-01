package voice.settings

data class SettingsViewState(
  val useDarkTheme: Boolean,
  val showDarkThemePref: Boolean,
  val resumeOnReplug: Boolean,
  val seekTimeInSeconds: Int,
  val autoRewindInSeconds: Int,
  val dialog: Dialog?,
) {

  companion object {
    val Empty = SettingsViewState(
      useDarkTheme = false,
      showDarkThemePref = false,
      resumeOnReplug = false,
      seekTimeInSeconds = 0,
      autoRewindInSeconds = 0,
      dialog = null,
    )
  }

  enum class Dialog {
    Contribute, AutoRewindAmount, SeekTime
  }
}

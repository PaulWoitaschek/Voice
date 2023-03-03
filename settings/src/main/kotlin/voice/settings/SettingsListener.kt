package voice.settings

interface SettingsListener {
  fun close()
  fun toggleDarkTheme()
  fun toggleGrid()
  fun seekAmountChanged(seconds: Int)
  fun onSeekAmountRowClicked()
  fun autoRewindAmountChanged(seconds: Int)
  fun onAutoRewindRowClicked()
  fun dismissDialog()
  fun getSupport()
  fun suggestIdea()
  fun openBugReport()
  fun openTranslations()
}

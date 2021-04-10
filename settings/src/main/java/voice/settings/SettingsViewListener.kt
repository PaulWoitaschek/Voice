package voice.settings

interface SettingsViewListener {
  fun close()
  fun toggleResumeOnReplug()
  fun toggleDarkTheme()
  fun seekAmountChanged(seconds: Int)
  fun autoRewindAmountChanged(seconds: Int)
  fun openSupport()
  fun openTranslations()
}

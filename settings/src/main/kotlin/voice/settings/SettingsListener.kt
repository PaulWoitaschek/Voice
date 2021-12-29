package voice.settings

interface SettingsListener {
  fun close()
  fun toggleResumeOnReplug()
  fun toggleDarkTheme()
  fun seekAmountChanged(seconds: Int)
  fun onSeekAmountRowClicked()
  fun autoRewindAmountChanged(seconds: Int)
  fun onAutoRewindRowClicked()
  fun onLikeClicked()
  fun dismissDialog()
  fun openSupport()
  fun openTranslations()
}

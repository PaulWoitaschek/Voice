package de.ph1b.audiobook.features.settings

sealed class SettingsViewEffect {

  data class ShowChangeSkipAmountDialog(val amountInSeconds: Int) : SettingsViewEffect()
  data class ShowChangeAutoRewindAmountDialog(val amountInSeconds: Int) : SettingsViewEffect()
}

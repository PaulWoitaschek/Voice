package voice.features.settings

internal sealed interface SettingsViewEffect {
  data object DeveloperMenuUnlocked : SettingsViewEffect
}

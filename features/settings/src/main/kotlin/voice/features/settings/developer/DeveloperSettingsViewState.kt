package voice.features.settings.developer

data class DeveloperSettingsViewState(
  val fcmToken: String?,
  val featureFlags: List<FeatureFlagViewState>,
) {
  data class FeatureFlagViewState(
    val key: String,
    val value: String,
  )
}

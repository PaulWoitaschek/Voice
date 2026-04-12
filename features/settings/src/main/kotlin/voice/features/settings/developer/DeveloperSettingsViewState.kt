package voice.features.settings.developer

data class DeveloperSettingsViewState(
  val fcmToken: String?,
  val featureFlags: List<FeatureFlag>,
) {
  data class FeatureFlag(
    val key: String,
    val value: String,
  )
}

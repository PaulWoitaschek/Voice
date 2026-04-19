package voice.features.settings.developer

data class DeveloperSettingsViewState(
  val fcmToken: String?,
  val featureFlags: List<FeatureFlagViewState>,
) {
  sealed interface FeatureFlagViewState {
    val key: String
    val description: String
    val isOverridden: Boolean

    data class BooleanFlag(
      override val key: String,
      override val description: String,
      val value: Boolean,
      override val isOverridden: Boolean,
    ) : FeatureFlagViewState

    data class StringFlag(
      override val key: String,
      override val description: String,
      val value: String,
      override val isOverridden: Boolean,
    ) : FeatureFlagViewState
  }
}

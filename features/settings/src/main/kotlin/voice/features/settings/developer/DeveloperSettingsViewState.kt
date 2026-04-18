package voice.features.settings.developer

data class DeveloperSettingsViewState(
  val fcmToken: String?,
  val featureFlags: List<FeatureFlagViewState>,
) {
  sealed interface FeatureFlagViewState {
    val key: String
    val isOverridden: Boolean

    data class BooleanFlag(
      override val key: String,
      val value: Boolean,
      override val isOverridden: Boolean,
    ) : FeatureFlagViewState

    data class StringFlag(
      override val key: String,
      val value: String,
      override val isOverridden: Boolean,
    ) : FeatureFlagViewState
  }
}

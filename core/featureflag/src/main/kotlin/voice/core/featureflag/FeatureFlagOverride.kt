package voice.core.featureflag

import kotlinx.serialization.Serializable

@Serializable
sealed interface FeatureFlagOverride {
  @Serializable
  data class BooleanValue(val value: Boolean) : FeatureFlagOverride

  @Serializable
  data class StringValue(val value: String) : FeatureFlagOverride
}

package voice.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class ThemeColorScheme {
  @SerialName("VoiceBlue")
  VoiceBlue,

  @SerialName("Dynamic")
  Dynamic,

  @SerialName("Lagoon")
  Lagoon,

  @SerialName("Forest")
  Forest,

  @SerialName("Sunset")
  Sunset,

  @SerialName("Berry")
  Berry,

  @SerialName("Lavender")
  Lavender,
}

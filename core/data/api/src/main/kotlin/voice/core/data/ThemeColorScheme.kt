package voice.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class ThemeColorScheme {
  @SerialName("VoiceBlue")
  VoiceBlue,

  @SerialName("Dynamic")
  Dynamic,
}

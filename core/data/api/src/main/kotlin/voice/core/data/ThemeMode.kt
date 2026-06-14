package voice.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class ThemeMode {
  @SerialName("FollowSystem")
  FollowSystem,

  @SerialName("light")
  Light,

  @SerialName("dark")
  Dark,
}

package voice.core.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
  @SerialName("FollowSystem")
  FollowSystem,

  @SerialName("light")
  Light,

  @SerialName("dark")
  Dark,
}

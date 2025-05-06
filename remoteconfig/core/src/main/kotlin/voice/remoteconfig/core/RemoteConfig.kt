package voice.remoteconfig.core

interface RemoteConfig {
  fun boolean(
    key: String,
    defaultValue: Boolean = false,
  ): Boolean
  fun string(
    key: String,
    defaultValue: String,
  ): String
}

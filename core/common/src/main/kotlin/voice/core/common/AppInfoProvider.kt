package voice.core.common

interface AppInfoProvider {
  val versionName: String

  val analyticsIncluded: Boolean
}

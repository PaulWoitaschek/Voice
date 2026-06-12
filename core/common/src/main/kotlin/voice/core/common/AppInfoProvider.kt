package voice.core.common

import kotlin.time.Instant

interface AppInfoProvider {
  val versionName: String

  val analyticsIncluded: Boolean

  val supportDevelopmentIncluded: Boolean

  val installTime: Instant
}

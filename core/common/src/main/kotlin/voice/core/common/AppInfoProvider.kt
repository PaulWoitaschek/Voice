package voice.core.common

import kotlin.time.Instant

interface AppInfoProvider {
  val versionName: String

  val variant: AppVariant

  val analyticsIncluded: Boolean

  val installTime: Instant

  val installer: String?
}

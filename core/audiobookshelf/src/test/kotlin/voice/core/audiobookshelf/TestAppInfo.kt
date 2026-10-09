package voice.core.audiobookshelf

import voice.core.common.AppInfoProvider
import kotlin.time.Instant

object TestAppInfo : AppInfoProvider {
  override val versionName: String = "1.0.0"
  override val analyticsIncluded: Boolean = false
  override val supportDevelopmentIncluded: Boolean = false
  override val installTime: Instant = Instant.fromEpochMilliseconds(0)
}

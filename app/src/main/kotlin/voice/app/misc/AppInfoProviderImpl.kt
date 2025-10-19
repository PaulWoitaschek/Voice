package voice.app.misc

import dev.zacsweers.metro.Inject
import voice.app.BuildConfig
import voice.core.common.AppInfoProvider

@Inject
class AppInfoProviderImpl : AppInfoProvider {
  override val versionName: String = BuildConfig.VERSION_NAME
  override val analyticsIncluded: Boolean = BuildConfig.INCLUDE_ANALYTICS
}

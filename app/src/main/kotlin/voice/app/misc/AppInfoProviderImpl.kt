package voice.app.misc

import dev.zacsweers.metro.Inject
import voice.app.BuildConfig
import voice.common.AppInfoProvider

@Inject
class AppInfoProviderImpl : AppInfoProvider {

  override val applicationID = BuildConfig.APPLICATION_ID

  override val versionName: String = BuildConfig.VERSION_NAME
}

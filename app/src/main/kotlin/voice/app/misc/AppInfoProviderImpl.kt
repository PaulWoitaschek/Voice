package voice.app.misc

import javax.inject.Inject
import voice.app.BuildConfig
import voice.common.AppInfoProvider

class AppInfoProviderImpl
@Inject constructor() : AppInfoProvider {

  override val applicationID = BuildConfig.APPLICATION_ID

  override val versionName: String = BuildConfig.VERSION_NAME
}

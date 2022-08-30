package voice.app.misc

import voice.app.BuildConfig
import voice.common.AppInfoProvider
import javax.inject.Inject

class AppInfoProviderImpl
@Inject constructor() : AppInfoProvider {

  override val applicationID = BuildConfig.APPLICATION_ID

  override val versionName: String = BuildConfig.VERSION_NAME
}

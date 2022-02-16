package voice.app.misc

import voice.app.BuildConfig
import voice.common.ApplicationIdProvider
import javax.inject.Inject

class ApplicationIdProviderImpl
@Inject constructor() : ApplicationIdProvider {

  override val applicationID = BuildConfig.APPLICATION_ID
}

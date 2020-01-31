package de.ph1b.audiobook.misc

import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.common.ApplicationIdProvider
import javax.inject.Inject

class ApplicationIdProviderImpl
@Inject constructor() : ApplicationIdProvider {

  override val applicationID = BuildConfig.APPLICATION_ID
}

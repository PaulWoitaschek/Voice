package voice.core.analytics.api

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
class AnalyticsConsentStore(store: DataStore<Boolean>) : DataStore<Boolean> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.boolean("analyticsConsent", defaultValue = false),
  )
}

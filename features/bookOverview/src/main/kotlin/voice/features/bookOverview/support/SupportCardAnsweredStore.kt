package voice.features.bookOverview.support

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
class SupportCardAnsweredStore(store: DataStore<Boolean>) : DataStore<Boolean> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
  ) : this(
    factory.boolean("supportCardAnswered", defaultValue = false),
  )
}

package voice.features.sibha

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
class SibhaCountStore(store: DataStore<Int>) : DataStore<Int> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(factory.int("sibhaCount", defaultValue = 0))
}

@SingleIn(AppScope::class)
class SibhaReminderStore(store: DataStore<Int>) : DataStore<Int> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) :
    this(factory.int("sibhaReminderEvery", defaultValue = 0))
}

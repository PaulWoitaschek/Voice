package voice.core.sleeptimer

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
class SleepTimerPreferenceStore(store: DataStore<SleepTimerPreference>) : DataStore<SleepTimerPreference> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.create(
      serializer = SleepTimerPreference.serializer(),
      fileName = "sleepTime3",
      defaultValue = SleepTimerPreference.Default,
    ),
  )
}

package voice.features.playbackScreen.batteryOptimization

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
class AmountOfBatteryOptimizationRequestedStore(store: DataStore<Int>) : DataStore<Int> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.int("amountOfBatteryOptimizationsRequestedStore", 0),
  )
}

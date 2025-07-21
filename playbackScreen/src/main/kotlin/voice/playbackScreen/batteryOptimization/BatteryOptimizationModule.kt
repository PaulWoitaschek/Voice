package voice.playbackScreen.batteryOptimization

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import voice.common.AppScope
import voice.datastore.VoiceDataStoreFactory

@BindingContainer
@ContributesTo(AppScope::class)
object BatteryOptimizationModule {

  @Provides
  @SingleIn(AppScope::class)
  fun amountOfBatteryOptimizationsRequestedStore(factory: VoiceDataStoreFactory): DataStore<Int> {
    return factory.int("amountOfBatteryOptimizationsRequestedStore", 0)
  }
}

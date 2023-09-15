package voice.playbackScreen.batteryOptimization

import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import voice.common.AppScope
import voice.datastore.VoiceDataStoreFactory
import javax.inject.Singleton

@Module
@ContributesTo(AppScope::class)
object BatteryOptimizationModule {

  @Provides
  @Singleton
  fun amountOfBatteryOptimizationsRequestedStore(factory: VoiceDataStoreFactory): DataStore<Int> {
    return factory.int("amountOfBatteryOptimizationsRequestedStore", 0)
  }
}

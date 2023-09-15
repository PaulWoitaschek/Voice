package voice.playbackScreen.batteryOptimization

import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import voice.common.AppScope
import voice.datastore.VoiceDataStoreFactory

@Module
@ContributesTo(AppScope::class)
object BatteryOptimizationModule {

  @Provides
  @Singleton
  fun amountOfBatteryOptimizationsRequestedStore(factory: VoiceDataStoreFactory): DataStore<Int> {
    return factory.int("amountOfBatteryOptimizationsRequestedStore", 0)
  }
}

package voice.playbackScreen.batteryOptimization

import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.first
import voice.common.AppScope
import voice.datastore.VoiceDataStoreFactory
import javax.inject.Inject
import javax.inject.Singleton

class BatteryOptimization
@Inject constructor(
  private val isIgnoringBatteryOptimizations: IsIgnoringBatteryOptimizations,
  private val amountOfBatteryOptimizationsRequested: DataStore<Int>,
) {

  suspend fun shouldRequest(): Boolean = if (isIgnoringBatteryOptimizations()) {
    false
  } else {
    amountOfBatteryOptimizationsRequested.data.first() <= 3
  }

  suspend fun onBatteryOptimizationsRequested() {
    amountOfBatteryOptimizationsRequested.updateData { it + 1 }
  }
}

@Module
@ContributesTo(AppScope::class)
object BatteryOptimizationModule {

  @Provides
  @Singleton
  fun amountOfBatteryOptimizationsRequestedStore(factory: VoiceDataStoreFactory): DataStore<Int> {
    return factory.int("amountOfBatteryOptimizationsRequestedStore", 0)
  }
}

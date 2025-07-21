package voice.playbackScreen.batteryOptimization

import android.os.Build
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

@Inject
class BatteryOptimization(
  private val isIgnoringBatteryOptimizations: IsIgnoringBatteryOptimizations,
  private val amountOfBatteryOptimizationsRequested: DataStore<Int>,
) {

  suspend fun shouldRequest(): Boolean = when {
    isIgnoringBatteryOptimizations() -> false
    Build.MANUFACTURER.equals("Google", ignoreCase = true) -> false
    else -> amountOfBatteryOptimizationsRequested.data.first() <= 3
  }

  suspend fun onBatteryOptimizationsRequested() {
    amountOfBatteryOptimizationsRequested.updateData { it + 1 }
  }
}

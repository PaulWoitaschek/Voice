package voice.playbackScreen.batteryOptimization

import androidx.datastore.core.DataStore
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BatteryOptimizationTest {

  @Test
  fun `requests at most three times`() = runTest {
    val batteryOptimization = BatteryOptimization(
      isIgnoringBatteryOptimizations = { false },
      amountOfBatteryOptimizationsRequested = MemoryDataStore(0),
    )

    batteryOptimization.shouldRequest().shouldBeTrue()

    batteryOptimization.onBatteryOptimizationsRequested()
    batteryOptimization.shouldRequest().shouldBeTrue()

    batteryOptimization.onBatteryOptimizationsRequested()
    batteryOptimization.shouldRequest().shouldBeTrue()

    batteryOptimization.onBatteryOptimizationsRequested()
    batteryOptimization.shouldRequest().shouldBeTrue()

    batteryOptimization.onBatteryOptimizationsRequested()
    batteryOptimization.shouldRequest().shouldBeFalse()
  }

  @Test
  fun `does not request when optimizations are ignored`() = runTest {
    val batteryOptimization = BatteryOptimization(
      isIgnoringBatteryOptimizations = { true },
      amountOfBatteryOptimizationsRequested = MemoryDataStore(0),
    )

    batteryOptimization.shouldRequest().shouldBeFalse()
  }
}

private class MemoryDataStore<T>(
  initial: T,
) : DataStore<T> {

  private val value = MutableStateFlow(initial)

  override val data: Flow<T> get() = value

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    return value.updateAndGet { transform(it) }
  }
}

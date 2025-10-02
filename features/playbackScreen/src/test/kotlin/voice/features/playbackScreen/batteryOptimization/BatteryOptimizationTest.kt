package voice.features.playbackScreen.batteryOptimization

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.features.playbackScreen.MemoryDataStore

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

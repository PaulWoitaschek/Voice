package voice.features.playbackScreen.batteryOptimization

import kotlinx.coroutines.test.runTest
import voice.features.playbackScreen.MemoryDataStore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatteryOptimizationTest {

  @Test
  fun `requests at most three times`() = runTest {
    val batteryOptimization = BatteryOptimization(
      isIgnoringBatteryOptimizations = { false },
      amountOfBatteryOptimizationsRequested = MemoryDataStore(0),
    )

    assertTrue(batteryOptimization.shouldRequest())

    batteryOptimization.onBatteryOptimizationsRequested()
    assertTrue(batteryOptimization.shouldRequest())

    batteryOptimization.onBatteryOptimizationsRequested()
    assertTrue(batteryOptimization.shouldRequest())

    batteryOptimization.onBatteryOptimizationsRequested()
    assertTrue(batteryOptimization.shouldRequest())

    batteryOptimization.onBatteryOptimizationsRequested()
    assertFalse(batteryOptimization.shouldRequest())
  }

  @Test
  fun `does not request when optimizations are ignored`() = runTest {
    val batteryOptimization = BatteryOptimization(
      isIgnoringBatteryOptimizations = { true },
      amountOfBatteryOptimizationsRequested = MemoryDataStore(0),
    )

    assertFalse(batteryOptimization.shouldRequest())
  }
}

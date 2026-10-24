package voice.core.featureflag

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.remoteconfig.api.RemoteConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagFactoryTest {

  private enum class Style { Classic, Calm }

  private val remoteConfig = mockk<RemoteConfig>()
  private val overridesStore = MemoryDataStore<Map<String, FeatureFlagOverride>>(emptyMap())

  private fun TestScope.styleFlag(remoteValue: String): FeatureFlag<Style> {
    every { remoteConfig.string("style", "Classic") } returns remoteValue
    return FeatureFlagFactory(remoteConfig, FeatureFlagOverridesStore(overridesStore), backgroundScope)
      .enum(key = "style", description = "", defaultValue = Style.Classic, entries = Style.entries)
  }

  @Test
  fun `enum flag reads the remote config entry by name`() = runTest {
    assertEquals(Style.Calm, styleFlag(remoteValue = "Calm").get())
  }

  @Test
  fun `enum flag falls back to the default for an unknown remote value`() = runTest {
    assertEquals(Style.Classic, styleFlag(remoteValue = "Unknown").get())
  }

  @Test
  fun `enum flag override wins over the remote config`() = runTest {
    val flag = styleFlag(remoteValue = "Classic")

    flag.overwrite(Style.Calm)
    runCurrent()

    assertEquals(Style.Calm, flag.get())
    assertEquals(FeatureFlagValue(Style.Calm, isOverridden = true), flag.flow.first())
  }

  @Test
  fun `enum flag ignores an override of an entry that no longer exists`() = runTest {
    overridesStore.updateData { mapOf("style" to FeatureFlagOverride.StringValue("Removed")) }

    val flag = styleFlag(remoteValue = "Calm")

    assertEquals(FeatureFlagValue(Style.Calm, isOverridden = false), flag.flow.first())
  }
}

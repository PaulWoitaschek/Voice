package voice.features.support

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import voice.core.common.AppInfoProvider
import voice.core.common.AppVariant
import voice.core.featureflag.FeatureFlagFactory
import voice.core.featureflag.MemoryFeatureFlag

class SupporterCountFeatureFlagTest {

  private val factory =
    mockk<FeatureFlagFactory> {
      every { long("supporter_count", any(), any()) } answers
        {
          MemoryFeatureFlag(thirdArg<Long>())
        }
    }

  private fun count(variant: AppVariant): Long {
    val appInfoProvider = mockk<AppInfoProvider> { every { this@mockk.variant } returns variant }
    return SupporterCountFeatureFlag(factory, appInfoProvider).get()
  }

  @Test
  fun `the foss build shows the count of the release`() {
    assertEquals(expected = 200, actual = count(AppVariant.Foss))
  }

  @Test
  fun `the play build hides the count until remote config sets it`() {
    assertEquals(expected = 0, actual = count(AppVariant.Play))
  }
}

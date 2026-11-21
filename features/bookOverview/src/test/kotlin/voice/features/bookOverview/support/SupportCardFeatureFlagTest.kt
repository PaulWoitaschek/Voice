package voice.features.bookOverview.support

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import voice.core.featureflag.FeatureFlagFactory
import voice.core.featureflag.MemoryFeatureFlag

class SupportCardFeatureFlagTest {

  @Test
  fun `no remote setting, Voice shows no card`() {
    val factory =
      mockk<FeatureFlagFactory> {
        every { enum<SupportCardLook>("support_card_after_book", any(), any(), any()) } answers
          {
            MemoryFeatureFlag(thirdArg<SupportCardLook>())
          }
      }

    assertEquals(expected = SupportCardLook.None, actual = SupportCardFeatureFlag(factory).get())
  }
}

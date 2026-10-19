package voice.core.featureflag

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingExperimentTest {

  private val flag = MemoryFeatureFlag(OnboardingVariant.Control.key)
  private val experiment = OnboardingExperiment(flag)

  @Test
  fun `the variant reads the remote config until the onboarding starts`() {
    flag.value = OnboardingVariant.Guided.key

    assertEquals(expected = OnboardingVariant.Guided, actual = experiment.variant)
  }

  @Test
  fun `a remote config arriving after the start doesn't switch the variant`() {
    val _ = experiment.start()
    flag.value = OnboardingVariant.Guided.key

    assertEquals(expected = OnboardingVariant.Control, actual = experiment.variant)
  }

  @Test
  fun `an unknown variant falls back to the control`() {
    flag.value = "unknown"

    assertEquals(expected = OnboardingVariant.Control, actual = experiment.start())
  }
}

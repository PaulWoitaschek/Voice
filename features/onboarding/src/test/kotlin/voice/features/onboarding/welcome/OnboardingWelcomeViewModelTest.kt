package voice.features.onboarding.welcome

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.common.DispatcherProvider
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.featureflag.OnboardingExperiment
import voice.core.featureflag.OnboardingVariant
import voice.features.onboarding.MemoryDataStore
import voice.navigation.Destination
import voice.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingWelcomeViewModelTest {

  private val scope = TestScope()
  private val navigator = mockk<Navigator>(relaxUnitFun = true)
  private val analytics = mockk<Analytics>(relaxUnitFun = true)
  private val analyticsConsent = MemoryDataStore(false)
  private val variant = MemoryFeatureFlag(OnboardingVariant.Control.key)
  private val viewModel = OnboardingWelcomeViewModel(
    navigator = navigator,
    analyticsConsentStore = analyticsConsent,
    appInfoProvider = mockk(),
    onboardingExperiment = OnboardingExperiment(variant),
    analytics = analytics,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun `continuing with analytics stores the consent and starts the variant`() = scope.runTest {
    variant.value = OnboardingVariant.Guided.key

    viewModel.onContinueWithAnalytics()
    runCurrent()

    assertEquals(expected = true, actual = analyticsConsent.data.first())
    verify {
      analytics.event("onboarding_start", mapOf("variant" to "guided"))
      navigator.goTo(Destination.OnboardingGuide)
    }
  }

  @Test
  fun `continuing without analytics starts the control`() = scope.runTest {
    viewModel.onContinueWithoutAnalytics()
    runCurrent()

    assertEquals(expected = false, actual = analyticsConsent.data.first())
    verify {
      analytics.event("onboarding_start", mapOf("variant" to "control"))
      navigator.goTo(Destination.OnboardingExplanation)
    }
  }
}

package voice.features.onboarding.welcome

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.analytics.api.AnalyticsConsentStore
import voice.core.common.DispatcherProvider
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
  private val viewModel = OnboardingWelcomeViewModel(
    navigator = navigator,
    analyticsConsentStore = AnalyticsConsentStore(analyticsConsent),
    appInfoProvider = mockk(),
    analytics = analytics,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun `continuing with analytics stores the consent before starting`() = scope.runTest {
    viewModel.onContinueWithAnalytics()
    runCurrent()

    assertEquals(expected = true, actual = analyticsConsent.data.first())
    verify {
      analytics.event("onboarding_start")
      navigator.goTo(Destination.OnboardingExplanation)
    }
  }

  @Test
  fun `continuing without analytics withdraws an earlier consent`() = scope.runTest {
    analyticsConsent.updateData { true }

    viewModel.onContinueWithoutAnalytics()
    runCurrent()

    assertEquals(expected = false, actual = analyticsConsent.data.first())
    verify { navigator.goTo(Destination.OnboardingExplanation) }
  }
}

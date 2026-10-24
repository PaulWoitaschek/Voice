package voice.features.onboarding.welcome

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import voice.core.analytics.api.Analytics
import voice.core.analytics.api.AnalyticsConsentStore
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.navigation.Destination
import voice.navigation.Navigator

@Inject
class OnboardingWelcomeViewModel(
  private val navigator: Navigator,
  private val analyticsConsentStore: AnalyticsConsentStore,
  private val appInfoProvider: AppInfoProvider,
  private val analytics: Analytics,
  dispatcherProvider: DispatcherProvider,
) {

  private val scope = MainScope(dispatcherProvider)

  fun viewState(): OnboardingWelcomeViewState {
    return OnboardingWelcomeViewState(
      askForAnalytics = appInfoProvider.analyticsIncluded,
    )
  }

  fun onContinueWithAnalytics() {
    start(analyticsConsent = true)
  }

  fun onContinueWithoutAnalytics() {
    start(analyticsConsent = false)
  }

  fun onPrivacyPolicyClick() {
    navigator.goTo(Destination.Website("https://voice.woitaschek.de/privacy-policy"))
  }

  private fun start(analyticsConsent: Boolean) {
    scope.launch {
      analyticsConsentStore.updateData { analyticsConsent }
      // logged after the consent is stored, so the funnel starts with every listener who agreed
      analytics.event("onboarding_start")
      navigator.goTo(Destination.OnboardingExplanation)
    }
  }
}

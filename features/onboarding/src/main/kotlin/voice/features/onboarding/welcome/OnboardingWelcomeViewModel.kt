package voice.features.onboarding.welcome

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import voice.core.analytics.api.Analytics
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.store.AnalyticsConsentStore
import voice.core.featureflag.OnboardingExperiment
import voice.core.featureflag.OnboardingVariant
import voice.navigation.Destination
import voice.navigation.Navigator

@Inject
class OnboardingWelcomeViewModel(
  private val navigator: Navigator,
  @AnalyticsConsentStore
  private val analyticsConsentStore: DataStore<Boolean>,
  private val appInfoProvider: AppInfoProvider,
  private val onboardingExperiment: OnboardingExperiment,
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

  // the variants split only after the consent, so they are compared among the same listeners
  private fun start(analyticsConsent: Boolean) {
    scope.launch {
      analyticsConsentStore.updateData { analyticsConsent }
      val variant = onboardingExperiment.start()
      analytics.event("onboarding_start", mapOf("variant" to variant.key))
      navigator.goTo(
        when (variant) {
          OnboardingVariant.Control -> Destination.OnboardingExplanation
          OnboardingVariant.Guided -> Destination.OnboardingGuide
        },
      )
    }
  }
}

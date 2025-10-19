package voice.features.onboarding.welcome

import dev.zacsweers.metro.Inject
import voice.navigation.Destination
import voice.navigation.Navigator

@Inject
class OnboardingWelcomeViewModel(private val navigator: Navigator) {

  fun next() {
    navigator.goTo(Destination.OnboardingExplanation)
  }
}

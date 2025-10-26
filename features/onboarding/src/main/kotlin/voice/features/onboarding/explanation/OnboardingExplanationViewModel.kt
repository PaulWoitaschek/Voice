package voice.features.onboarding.explanation

import dev.zacsweers.metro.Inject
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin

@Inject
class OnboardingExplanationViewModel(private val navigator: Navigator) {

  fun onNext() {
    navigator.goTo(Destination.AddContent(Origin.Onboarding))
  }

  fun onClose() {
    navigator.goBack()
  }
}

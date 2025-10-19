package voice.features.onboarding.explanation

import dev.zacsweers.metro.Inject
import voice.navigation.Destination
import voice.navigation.Navigator

@Inject
class OnboardingExplanationViewModel(private val navigator: Navigator) {

  fun onNext() {
    navigator.goTo(Destination.AddContent(mode = Destination.AddContent.Mode.Onboarding))
  }

  fun onClose() {
    navigator.goBack()
  }
}

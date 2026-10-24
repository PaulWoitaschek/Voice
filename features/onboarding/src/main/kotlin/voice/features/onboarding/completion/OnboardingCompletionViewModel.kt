package voice.features.onboarding.completion

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.features.onboarding.OnboardingCompletedStore
import voice.navigation.Destination
import voice.navigation.Navigator

@Inject
class OnboardingCompletionViewModel(
  private val onboardingCompletedStore: OnboardingCompletedStore,
  private val navigator: Navigator,
) {

  private val scope = MainScope()

  fun next() {
    scope.launch {
      onboardingCompletedStore.updateData { true }
    }
    navigator.setRoot(Destination.BookOverview)
  }

  fun back() {
    navigator.goBack()
  }
}

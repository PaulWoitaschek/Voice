package voice.features.onboarding.completion

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.core.common.pref.OnboardingCompletedStore
import voice.navigation.Destination
import voice.navigation.Navigator

@Inject
class OnboardingCompletionViewModel(
  @OnboardingCompletedStore
  private val onboardingCompletedStore: DataStore<Boolean>,
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

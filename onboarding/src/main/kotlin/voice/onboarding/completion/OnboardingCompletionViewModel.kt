package voice.onboarding.completion

import androidx.datastore.core.DataStore
import dev.olshevski.navigation.reimagined.replaceAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.common.pref.OnboardingCompleted
import javax.inject.Inject

class OnboardingCompletionViewModel
@Inject constructor(
  @OnboardingCompleted
  private val onboardingCompleted: DataStore<Boolean>,
  private val navigator: Navigator,
) {

  private val scope = MainScope()

  fun next() {
    scope.launch {
      onboardingCompleted.updateData { true }
    }
    navigator.execute {
      replaceAll(Destination.BookOverview)
    }
  }

  fun back() {
    navigator.goBack()
  }
}

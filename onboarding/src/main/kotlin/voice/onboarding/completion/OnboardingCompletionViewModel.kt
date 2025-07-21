package voice.onboarding.completion

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.common.pref.OnboardingCompletedStore
import com.kiwi.navigationcompose.typed.navigate as typedNavigate

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
    navigator.execute { navController ->
      navController.typedNavigate(Destination.BookOverview) {
        popUpTo(0)
      }
    }
  }

  fun back() {
    navigator.goBack()
  }
}

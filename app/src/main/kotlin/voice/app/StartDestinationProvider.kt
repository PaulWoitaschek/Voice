package voice.app

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import voice.common.navigation.Destination
import voice.common.pref.OnboardingCompleted
import javax.inject.Inject

class StartDestinationProvider
@Inject constructor(
  @OnboardingCompleted
  private val onboardingCompleted: DataStore<Boolean>,
) {

  operator fun invoke(): Destination.Compose {
    return runBlocking {
      if (onboardingCompleted.data.first()) {
        Destination.BookOverview
      } else {
        Destination.OnboardingWelcome
      }
    }
  }
}

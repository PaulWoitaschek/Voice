package voice.app

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import voice.common.navigation.Destination
import voice.common.pref.OnboardingCompleted
import voice.data.folders.AudiobookFolders
import javax.inject.Inject

class StartDestinationProvider
@Inject constructor(
  @OnboardingCompleted
  private val onboardingCompleted: DataStore<Boolean>,
  private val audiobookFolders: AudiobookFolders,
) {

  operator fun invoke(): Destination.Compose {
    return runBlocking {
      if (showOnboarding()) {
        Destination.OnboardingWelcome
      } else {
        Destination.BookOverview
      }
    }
  }

  private suspend fun showOnboarding(): Boolean {
    return when {
      onboardingCompleted.data.first() -> false
      audiobookFolders.hasAnyFolders() -> false
      else -> true
    }
  }
}

package voice.app

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import voice.common.pref.OnboardingCompletedStore
import voice.data.folders.AudiobookFolders
import javax.inject.Inject

class StartDestinationProvider
@Inject constructor(
  @OnboardingCompletedStore
  private val onboardingCompletedStore: DataStore<Boolean>,
  private val audiobookFolders: AudiobookFolders,
) {

  operator fun invoke(): StartDestination {
    return runBlocking {
      if (showOnboarding()) {
        StartDestination.OnboardingWelcome
      } else {
        StartDestination.BookOverview
      }
    }
  }

  private suspend fun showOnboarding(): Boolean {
    return when {
      onboardingCompletedStore.data.first() -> false
      audiobookFolders.hasAnyFolders() -> false
      else -> true
    }
  }

  enum class StartDestination {
    OnboardingWelcome,
    BookOverview,
  }
}

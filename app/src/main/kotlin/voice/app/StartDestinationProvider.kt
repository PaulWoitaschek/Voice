package voice.app

import android.content.Intent
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import voice.app.features.MainActivity.Companion.NI_GO_TO_BOOK
import voice.core.data.BookId
import voice.core.data.folders.AudiobookFolders
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.OnboardingCompletedStore
import voice.core.playback.PlayerController
import voice.navigation.Destination

@Inject
class StartDestinationProvider(
  @OnboardingCompletedStore
  private val onboardingCompletedStore: DataStore<Boolean>,
  private val audiobookFolders: AudiobookFolders,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val playerController: PlayerController,
) {

  operator fun invoke(intent: Intent): List<Destination.Compose> {
    val showOnboarding = runBlocking { showOnboarding() }
    if (showOnboarding) {
      return listOf(Destination.OnboardingWelcome)
    }

    val goToBook = intent.getBooleanExtra(NI_GO_TO_BOOK, false)
    if (goToBook) {
      val bookId = runBlocking { currentBookStore.data.first() }
      if (bookId != null) {
        return listOf(Destination.BookOverview, Destination.Playback(bookId))
      }
    }

    if (intent.action == "playCurrent") {
      val bookId = runBlocking { currentBookStore.data.first() }
      if (bookId != null) {
        playerController.play()
        return listOf(Destination.BookOverview, Destination.Playback(bookId))
      }
    }
    return listOf(Destination.BookOverview)
  }

  private suspend fun showOnboarding(): Boolean {
    return when {
      onboardingCompletedStore.data.first() -> false
      audiobookFolders.hasAnyFolders() -> false
      else -> true
    }
  }
}

package voice.app.navigation

import android.content.Intent
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import voice.app.BuildConfig
import voice.app.MainActivity
import voice.core.audiobookshelf.Audiobookshelf
import voice.core.data.BookId
import voice.core.data.folders.AudiobookFolders
import voice.core.data.store.CurrentBookStore
import voice.core.playback.PlayerController
import voice.features.onboarding.OnboardingCompletedStore
import voice.navigation.Destination

@Inject
class StartDestinationProvider(
  private val onboardingCompletedStore: OnboardingCompletedStore,
  private val audiobookFolders: AudiobookFolders,
  private val currentBookStore: CurrentBookStore,
  private val playerController: PlayerController,
  private val audiobookshelf: Audiobookshelf,
  private val json: Json,
) {

  operator fun invoke(intent: Intent): List<Destination.Compose> {
    debugBackStack(intent)?.let { return it }

    val showOnboarding = runBlocking { showOnboarding() }
    if (showOnboarding) {
      return listOf(Destination.OnboardingWelcome)
    }

    val goToBook = intent.getBooleanExtra(MainActivity.Companion.NI_GO_TO_BOOK, false)
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

  // Lets debug builds start on any screen, see the emulator skill. Release builds ignore it, since the activity is exported.
  private fun debugBackStack(intent: Intent): List<Destination.Compose>? {
    if (!BuildConfig.DEBUG) return null
    val backStack = intent.getStringExtra(MainActivity.DEBUG_BACK_STACK) ?: return null
    return json.decodeFromString<List<Destination.Compose>>(backStack).ifEmpty { null }
  }

  private suspend fun showOnboarding(): Boolean {
    return when {
      onboardingCompletedStore.data.first() -> false
      audiobookFolders.hasAnyFolders() -> false
      audiobookshelf.isConnected() -> false
      else -> true
    }
  }
}

package voice.common.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import kotlinx.serialization.Serializable
import voice.common.BookId
import voice.common.serialization.UriSerializer

sealed interface NavigationCommand {
  data object GoBack : NavigationCommand
  data class GoTo(val destination: Destination) : NavigationCommand
  data class Execute(val action: (NavController) -> Unit) : NavigationCommand
}

sealed interface Destination {
  data class Playback(val bookId: BookId) : Destination
  data class Bookmarks(val bookId: BookId) : Destination

  @Serializable
  data class CoverFromInternet(val bookId: BookId) : Compose
  data class Website(val url: String) : Destination
  data class EditCover(
    val bookId: BookId,
    val cover: Uri,
  ) : Destination

  data class Activity(val intent: Intent) : Destination

  @Serializable
  sealed interface Compose :
    Destination,
    com.kiwi.navigationcompose.typed.Destination

  @Serializable
  data object Migration : Compose

  @Serializable
  data object Settings : Compose

  @Serializable
  data object BookOverview : Compose

  @Serializable
  data object FolderPicker : Compose

  @Serializable
  data class SelectFolderType(
    val uri:
    @Serializable(with = UriSerializer::class)
    Uri,
    val mode: Mode,
  ) : Compose {

    enum class Mode {
      Default,
      Onboarding,
    }
  }

  @Serializable
  data object OnboardingWelcome : Compose

  @Serializable
  data object OnboardingCompletion : Compose

  @Serializable
  data object OnboardingExplanation : Compose

  @Serializable
  data class AddContent(val mode: Mode) : Compose {

    enum class Mode {
      Default,
      Onboarding,
    }
  }

  @Serializable
  data object GoogleDrive : Compose
}

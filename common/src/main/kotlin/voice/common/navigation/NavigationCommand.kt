package voice.common.navigation

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import voice.common.BookId

sealed interface NavigationCommand {
  data object GoBack : NavigationCommand
  data class GoTo(
    val destination: Destination,
    val replace: Boolean,
  ) : NavigationCommand
}

sealed interface Destination {
  data class Playback(val bookId: BookId) : Destination
  data class Bookmarks(val bookId: BookId) : Destination

  @Parcelize
  data class CoverFromInternet(val bookId: BookId) : Compose
  data class Website(val url: String) : Destination
  data class EditCover(val bookId: BookId, val cover: Uri) : Destination

  data class Activity(val intent: Intent) : Destination

  sealed interface Compose : Destination, Parcelable

  @Parcelize
  object Migration : Compose

  @Parcelize
  object Settings : Compose

  @Parcelize
  object BookOverview : Compose

  @Parcelize
  object FolderPicker : Compose

  @Parcelize
  data class SelectFolderType(val uri: Uri, val mode: Mode) : Compose {

    enum class Mode {
      Default,
      Onboarding,
    }
  }

  @Parcelize
  object OnboardingWelcome : Compose

  @Parcelize
  object OnboardingCompletion : Compose

  @Parcelize
  object OnboardingExplanation : Compose

  @Parcelize
  object OnboardingAddContent : Compose
}

package voice.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import voice.core.common.serialization.UriSerializer
import voice.core.data.BookId

sealed interface Destination {

  @Serializable
  data class Playback(val bookId: BookId) : Compose

  @Serializable
  data class Bookmarks(val bookId: BookId) : Compose

  @Serializable
  data class CoverFromInternet(val bookId: BookId) : Compose
  data class Website(val url: String) : Destination

  @Serializable
  data class EditCover(
    val bookId: BookId,
    val cover:
    @Serializable(with = UriSerializer::class)
    Uri,
  ) : Compose

  data class Activity(val intent: Intent) : Destination

  @Serializable
  sealed interface Compose :
    Destination,
    NavKey

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

  data object BatteryOptimization : Destination

  @Serializable
  data class AddContent(val mode: Mode) : Compose {

    enum class Mode {
      Default,
      Onboarding,
    }
  }
}

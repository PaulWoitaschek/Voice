package voice.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import voice.core.common.serialization.UriSerializer
import voice.core.data.BookId
import voice.core.data.folders.FolderType

sealed interface Destination {

  @Serializable
  data class Playback(val bookId: BookId) : Compose {
    override val trackingName: String get() = "Playback"
  }

  @Serializable
  data class Bookmarks(
    val bookId: BookId,
    /** Opens the editor for this bookmark, e.g. to add a note right after saving it. */
    val editBookmarkId: String? = null,
  ) : Compose {
    override val trackingName: String get() = "Bookmarks"
  }

  @Serializable
  data class BookActions(val bookId: BookId) : Compose {
    override val trackingName: String get() = "BookActions"
  }

  @Serializable
  data class CoverFromInternet(val bookId: BookId) : Compose {
    override val trackingName: String get() = "CoverFromInternet"
  }

  data class Website(val url: String) : Destination

  @Serializable
  data class EditCover(
    val bookId: BookId,
    val cover:
    @Serializable(with = UriSerializer::class)
    Uri,
  ) : Compose {
    override val trackingName: String get() = "EditCover"
  }

  data class Activity(val intent: Intent) : Destination

  @Serializable
  sealed interface Compose :
    Destination,
    NavKey {
    val trackingName: String
  }

  @Serializable
  data object Settings : Compose {
    override val trackingName: String get() = "Settings"
  }

  @Serializable
  data object SupportVoice : Compose {
    override val trackingName: String get() = "SupportVoice"
  }

  @Serializable
  data object DeveloperSettings : Compose {
    override val trackingName: String get() = "DeveloperSettings"
  }

  @Serializable
  data object BookOverview : Compose {
    override val trackingName: String get() = "BookOverview"
  }

  @Serializable
  data object LibrarySearch : Compose {
    override val trackingName: String get() = "LibrarySearch"
  }

  @Serializable
  data object FolderPicker : Compose {
    override val trackingName: String get() = "FolderPicker"
  }

  @Serializable
  data class SelectFolderType(
    val uri:
    @Serializable(with = UriSerializer::class)
    Uri,
    val origin: Origin,
    // set when changing the type of a folder that was added before
    val currentType: FolderType? = null,
  ) : Compose {
    override val trackingName: String = "SelectFolderType"
  }

  @Serializable
  data object OnboardingWelcome : Compose {
    override val trackingName: String get() = "OnboardingWelcome"
  }

  @Serializable
  data object OnboardingCompletion : Compose {
    override val trackingName: String get() = "OnboardingCompletion"
  }

  @Serializable
  data object OnboardingExplanation : Compose {
    override val trackingName: String get() = "OnboardingExplanation"
  }

  data object BatteryOptimization : Destination

  @Serializable
  data class AddContent(val origin: Origin) : Compose {
    override val trackingName: String = "AddContent"
  }
}

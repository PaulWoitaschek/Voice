package voice.common.navigation

import voice.common.BookId

sealed interface NavigationCommand {
  object GoBack : NavigationCommand
  data class GoTo(val destination: Destination) : NavigationCommand
}

sealed interface Destination {
  object PlaybackSpeedDialog : Destination
  data class Playback(val bookId: BookId) : Destination
  data class Bookmarks(val bookId: BookId) : Destination
  data class SelectChapterDialog(val bookId: BookId) : Destination
  data class CoverFromInternet(val bookId: BookId) : Destination
  data class CoverFromFiles(val bookId: BookId) : Destination
  data class Website(val url: String) : Destination

  sealed class Compose(val route: String) : Destination
  object Migration : Compose("migration")
  object Settings : Compose("settings")
  object BookOverview : Compose("bookOverview")
  object FolderPicker : Compose("folderPicker")
}

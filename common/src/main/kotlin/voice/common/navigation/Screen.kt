package voice.common.navigation

import voice.common.BookId

sealed class ComposeScreen(val route: String) : Screen
sealed interface ConductorScreen : Screen

sealed interface Screen {
  object Migration : ComposeScreen("migration")
  object Settings : ComposeScreen("settings")
  object BookOverview : ComposeScreen("bookOverview")
  object FolderPicker : ComposeScreen("folderPicker")
  object PlaybackSpeedDialog : ConductorScreen
  data class BookmarkDialog(val bookId: BookId) : ConductorScreen
  data class SelectChapterDialog(val bookId: BookId) : ConductorScreen
}

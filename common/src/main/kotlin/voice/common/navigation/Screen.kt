package voice.common.navigation

sealed class Screen(val route: String) {
  object Migration : Screen("migration")
  object Settings : Screen("settings")
  object BookOverview : Screen("bookOverview")
  object FolderPicker : Screen("folderPicker")
}

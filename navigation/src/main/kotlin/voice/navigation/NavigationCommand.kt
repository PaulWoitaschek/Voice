package voice.navigation

sealed interface NavigationCommand {
  data object GoBack : NavigationCommand
  data class GoTo(val destination: Destination) : NavigationCommand
  data class Remove(val destination: Destination.Compose) : NavigationCommand

  data class SetRoot(val root: Destination.Compose) : NavigationCommand
}

package voice.common.navigation

sealed interface NavigationCommand {
  data object GoBack : NavigationCommand
  data class GoTo(val destination: Destination) : NavigationCommand

  data class SetRoot(val root: Destination.Compose) : NavigationCommand
}


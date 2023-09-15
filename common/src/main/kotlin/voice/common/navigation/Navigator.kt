package voice.common.navigation

import dev.olshevski.navigation.reimagined.NavController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Navigator
@Inject constructor() {

  private val _navigationCommands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 10)
  val navigationCommands: Flow<NavigationCommand> get() = _navigationCommands

  private val scope = MainScope()

  fun goTo(
    destination: Destination,
    replace: Boolean = false,
  ) {
    scope.launch {
      _navigationCommands.emit(NavigationCommand.GoTo(destination, replace))
    }
  }

  fun goBack() {
    scope.launch {
      _navigationCommands.emit(NavigationCommand.GoBack)
    }
  }

  fun execute(action: NavController<Destination.Compose>.() -> Unit) {
    scope.launch {
      _navigationCommands.emit(NavigationCommand.Execute(action))
    }
  }
}

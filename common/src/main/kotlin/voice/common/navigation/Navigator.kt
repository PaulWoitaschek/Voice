package voice.common.navigation

import androidx.navigation.NavController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@Inject
class Navigator {

  private val _navigationCommands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 10)
  val navigationCommands: Flow<NavigationCommand> get() = _navigationCommands

  private val scope = MainScope()

  fun goTo(destination: Destination) {
    scope.launch {
      _navigationCommands.emit(NavigationCommand.GoTo(destination))
    }
  }

  fun goBack() {
    scope.launch {
      _navigationCommands.emit(NavigationCommand.GoBack)
    }
  }

  fun execute(action: (NavController) -> Unit) {
    scope.launch {
      _navigationCommands.emit(NavigationCommand.Execute(action))
    }
  }
}

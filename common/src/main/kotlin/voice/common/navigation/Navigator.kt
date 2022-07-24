package voice.common.navigation

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
}

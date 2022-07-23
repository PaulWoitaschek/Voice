package voice.common.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Navigator
@Inject constructor() {

  private val _navigationCommands = MutableSharedFlow<Screen>(extraBufferCapacity = 1)
  val composeCommands: Flow<ComposeScreen> get() = _navigationCommands.filterIsInstance()
  val conductorCommands: Flow<ConductorScreen> get() = _navigationCommands.filterIsInstance()

  fun toScreen(screen: Screen) {
    _navigationCommands.tryEmit(screen)
  }
}

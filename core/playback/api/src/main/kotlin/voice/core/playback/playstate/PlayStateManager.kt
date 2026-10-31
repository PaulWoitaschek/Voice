package voice.core.playback.playstate

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SingleIn(AppScope::class)
@Inject
class PlayStateManager {

  val playStateFlow: StateFlow<PlayState>
    field = MutableStateFlow(PlayState.Paused)

  var playState: PlayState
    set(value) {
      playStateFlow.value = value
    }
    get() = playStateFlow.value

  enum class PlayState {
    Playing,
    Paused,
  }
}

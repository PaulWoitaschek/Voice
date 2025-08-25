package voice.core.playback.playstate

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SingleIn(AppScope::class)
@Inject
class PlayStateManager {

  private val _playState = MutableStateFlow(PlayState.Paused)

  val flow: StateFlow<PlayState>
    get() = _playState

  var playState: PlayState
    set(value) {
      _playState.value = value
    }
    get() = _playState.value

  enum class PlayState {
    Playing,
    Paused,
  }
}

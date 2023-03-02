package voice.playback.playstate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayStateManager
@Inject
constructor() {

  private val _playState = MutableStateFlow(PlayState.Paused)

  val flow: Flow<PlayState>
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

package voice.playback.playstate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayStateManager
@Inject
constructor() {

  private val _playState = MutableStateFlow(PlayState.Stopped)

  var pauseReason = PauseReason.None

  val flow: Flow<PlayState>
    get() = _playState

  var playState: PlayState
    set(value) {
      if (_playState.value != value) {
        Timber.i("playState set to $value")
        _playState.value = value
        if (value == PlayState.Playing || value == PlayState.Stopped) {
          pauseReason = PauseReason.None
        }
      }
    }
    get() = _playState.value

  enum class PlayState {
    Playing,
    Paused,
    Stopped
  }

  enum class PauseReason {
    None,

    @Suppress("unused")
    Call,
    BecauseHeadset,

    @Suppress("unused")
    LossTransient
  }
}

package de.ph1b.audiobook.playback.playstate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the playback state and is able to inform subscriber.
 * Also manages the reason for pausing and sets it to none if the state gets stopped is playing.
 */
@Singleton
class PlayStateManager
@Inject
constructor() {

  private val playStateChannel = MutableStateFlow(PlayState.Stopped)

  init {
    @Suppress("CheckResult")
    GlobalScope.launch(Dispatchers.Main) {
      playStateChannel.collect {
        if (it == PlayState.Playing || it == PlayState.Stopped) {
          pauseReason = PauseReason.NONE
        }
      }
    }
  }

  var pauseReason = PauseReason.NONE

  fun playStateFlow(): Flow<PlayState> = playStateChannel

  var playState: PlayState
    set(value) {
      if (playStateChannel.value != value) {
        Timber.i("playState set to $value")
        playStateChannel.value = value
      }
    }
    get() = playStateChannel.value

  /** Represents the play states for the playback.  */
  enum class PlayState {
    Playing,
    Paused,
    Stopped
  }

  enum class PauseReason {
    NONE,

    @Suppress("unused")
    CALL,
    BECAUSE_HEADSET,

    @Suppress("unused")
    LOSS_TRANSIENT
  }
}

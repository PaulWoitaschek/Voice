package voice.core.playback.playstate

import androidx.media3.common.Player
import dev.zacsweers.metro.Inject

@Inject
class PlayStateDelegatingListener(private val playStateManager: PlayStateManager) : Player.Listener {
  private lateinit var player: Player

  fun attachTo(player: Player) {
    this.player = player
    player.addListener(this)
    updatePlayState()
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    updatePlayState()
  }

  override fun onPlayWhenReadyChanged(
    playWhenReady: Boolean,
    reason: Int,
  ) {
    updatePlayState()
  }

  private fun updatePlayState() {
    val playbackState = player.playbackState
    playStateManager.playState = when {
      playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE -> PlayStateManager.PlayState.Paused
      player.playWhenReady -> PlayStateManager.PlayState.Playing
      else -> PlayStateManager.PlayState.Paused
    }
  }
}

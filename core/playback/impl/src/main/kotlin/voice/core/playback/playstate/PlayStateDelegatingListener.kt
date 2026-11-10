package voice.core.playback.playstate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import voice.core.data.store.CurrentBookStore
import voice.core.playback.session.currentBookId

@Inject
class PlayStateDelegatingListener(
  private val playStateManager: PlayStateManager,
  private val currentBookStore: CurrentBookStore,
  private val scope: CoroutineScope,
) : Player.Listener {
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

  override fun onMediaItemTransition(
    mediaItem: MediaItem?,
    reason: Int,
  ) {
    updatePlayState()
  }

  private fun updatePlayState() {
    val playbackState = player.playbackState
    val bookId = player.currentBookId()
    val playState =
      when {
        playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE ->
          PlayStateManager.PlayState.Paused
        player.playWhenReady && bookId != null -> PlayStateManager.PlayState.Playing(bookId)
        else -> PlayStateManager.PlayState.Paused
      }
    if (
      playState is PlayStateManager.PlayState.Playing && playState != playStateManager.playState
    ) {
      // a headset or the notification resumes the book in the player, which can be another one
      scope.launch { currentBookStore.updateData { playState.bookId } }
    }
    playStateManager.playState = playState
  }
}

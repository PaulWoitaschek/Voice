package voice.playback.playstate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import voice.data.repo.ChapterRepo
import voice.playback.PlayerController
import voice.playback.session.MediaId
import voice.playback.session.toMediaIdOrNull
import javax.inject.Inject

class PlayStateDelegatingListener
@Inject constructor(
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  private val chapterRepo: ChapterRepo,
) : Player.Listener {
  private val scope = CoroutineScope(Dispatchers.Main.immediate)

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
    if (playStateManager.sleepAtEoc) {
      playStateManager.sleepAtEoc = false
      playerController.pauseAtStart()
    }
    if (player is ExoPlayer) {
      registerChapterMarkCallbacks(player as ExoPlayer)
    }
  }

  private fun updatePlayState() {
    val playbackState = player.playbackState
    playStateManager.playState = when {
      playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE -> PlayStateManager.PlayState.Paused
      player.playWhenReady -> PlayStateManager.PlayState.Playing
      else -> PlayStateManager.PlayState.Paused
    }
  }

  private fun registerChapterMarkCallbacks(player: ExoPlayer) {
    scope.launch {
      val currentMediaItem = player.currentMediaItem ?: return@launch
      val mediaId = currentMediaItem.mediaId.toMediaIdOrNull() ?: return@launch
      if (mediaId !is MediaId.Chapter) return@launch
      val marks = chapterRepo.get(mediaId.chapterId)?.chapterMarks?.filter { mark -> mark.startMs != 0L } ?: return@launch
      val boundaryHandler = PlayerMessage.Target { _, payload ->
        if (playStateManager.sleepAtEoc) {
          playerController.pauseAtTime(payload as Long)
          playStateManager.sleepAtEoc = false
        }
      }
      marks.forEach { mark ->
        player.createMessage(boundaryHandler)
          .setPosition(mark.startMs)
          .setPayload(mark.startMs)
          .setDeleteAfterDelivery(false)
          .send()
      }
    }
  }
}

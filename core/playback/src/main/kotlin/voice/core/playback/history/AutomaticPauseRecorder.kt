package voice.core.playback.history

import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.data.ListeningEvent
import voice.core.playback.di.PlaybackScope

/**
 * Records the pauses the player does on its own, when another app takes the audio focus
 * or when headphones are unplugged.
 */
@SingleIn(PlaybackScope::class)
@Inject
class AutomaticPauseRecorder(private val recorder: ListeningHistoryRecorder) {

  fun attachTo(player: Player) {
    player.addListener(
      object : Player.Listener {
        override fun onPlayWhenReadyChanged(
          playWhenReady: Boolean,
          reason: Int,
        ) {
          if (playWhenReady) return
          val source = when (reason) {
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> ListeningEvent.Source.AudioFocus
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> ListeningEvent.Source.Unplugged
            else -> return
          }
          val position = player.playbackPosition() ?: return
          recorder.record(
            type = ListeningEvent.Type.Pause,
            source = source,
            position = position,
          )
        }
      },
    )
  }
}

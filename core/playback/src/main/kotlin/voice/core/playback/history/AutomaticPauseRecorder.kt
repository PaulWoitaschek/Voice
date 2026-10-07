package voice.core.playback.history

import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.data.ListeningEvent
import voice.core.playback.di.PlaybackScope

/**
 * Records the pauses the player does on its own, when another app takes the audio focus
 * or when headphones are unplugged, and when it goes on after a call.
 */
@SingleIn(PlaybackScope::class)
@Inject
class AutomaticPauseRecorder(private val recorder: ListeningHistoryRecorder) {

  fun attachTo(player: Player) {
    player.addListener(
      object : Player.Listener {

        // a call or a navigation prompt holds playback back without pausing it, and it goes on afterwards
        private var heldBack = false

        override fun onPlayWhenReadyChanged(
          playWhenReady: Boolean,
          reason: Int,
        ) {
          if (playWhenReady) return
          val wasHeldBack = heldBack
          heldBack = false
          val source = when (reason) {
            // when holding back turns into a lasting loss, the pause was already recorded
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> if (wasHeldBack) return else ListeningEvent.Source.AudioFocus
            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> ListeningEvent.Source.Unplugged
            else -> return
          }
          record(player, ListeningEvent.Type.Pause, source)
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
          val held = playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
          if (held == heldBack || !player.playWhenReady) return
          heldBack = held
          record(
            player = player,
            type = if (held) ListeningEvent.Type.Pause else ListeningEvent.Type.Play,
            source = ListeningEvent.Source.AudioFocus,
          )
        }
      },
    )
  }

  private fun record(
    player: Player,
    type: ListeningEvent.Type,
    source: ListeningEvent.Source,
  ) {
    val position = player.playbackPosition() ?: return
    recorder.record(type = type, source = source, position = position)
  }
}

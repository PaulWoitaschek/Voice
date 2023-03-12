package voice.playback.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

internal fun Player.onAudioSessionIdChanged(action: (audioSessionId: Int?) -> Unit) {
  fun emitSessionId(id: Int) {
    action(id.takeUnless { it == C.AUDIO_SESSION_ID_UNSET })
  }
  if (this is ExoPlayer) {
    emitSessionId(audioSessionId)
  }
  addListener(
    object : Player.Listener {
      override fun onAudioSessionIdChanged(audioSessionId: Int) {
        emitSessionId(audioSessionId)
      }
    },
  )
}

package voice.playback.player

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import voice.playback.BuildConfig
import voice.playback.playstate.PlayerState

internal fun Player.onSessionPlaybackStateNeedsUpdate(listener: () -> Unit) {
  addListener(
    object : Player.Listener {
      override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        listener()
      }

      override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        listener()
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        listener()
      }

      override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        listener()
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        listener()
      }

      override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        listener()
      }
    },
  )
}

internal inline fun Player.onStateChanged(crossinline action: (PlayerState) -> Unit) {
  addListener(
    object : Player.Listener {

      override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        notifyListener(playbackState, playWhenReady)
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        notifyListener(playbackState, playWhenReady)
      }

      fun notifyListener(playbackState: Int, playWhenReady: Boolean) {
        val state = when (playbackState) {
          Player.STATE_ENDED -> PlayerState.ENDED
          Player.STATE_IDLE -> PlayerState.IDLE
          Player.STATE_READY, Player.STATE_BUFFERING -> {
            if (playWhenReady) {
              PlayerState.PLAYING
            } else {
              PlayerState.PAUSED
            }
          }
          else -> {
            if (BuildConfig.DEBUG) {
              error("Unknown playbackState $playbackState")
            }
            null
          }
        }
        if (state != null) {
          action(state)
        }
      }
    },
  )
}

internal inline fun Player.onError(crossinline action: (PlaybackException) -> Unit) {
  addListener(
    object : Player.Listener {
      override fun onPlayerError(error: PlaybackException) {
        action(error)
      }
    },
  )
}

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

internal inline fun Player.onPositionDiscontinuity(crossinline action: () -> Unit) {
  addListener(
    object : Player.Listener {
      override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        action()
      }
    },
  )
}

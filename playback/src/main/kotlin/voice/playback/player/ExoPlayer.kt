package voice.playback.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import voice.playback.BuildConfig
import voice.playback.playstate.PlayerState

fun ExoPlayer.onSessionPlaybackStateNeedsUpdate(listener: () -> Unit) {
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

inline fun ExoPlayer.onStateChanged(crossinline action: (PlayerState) -> Unit) {
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
            if (playWhenReady) PlayerState.PLAYING
            else PlayerState.PAUSED
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

inline fun ExoPlayer.onError(crossinline action: (PlaybackException) -> Unit) {
  addListener(
    object : Player.Listener {
      override fun onPlayerError(error: PlaybackException) {
        action(error)
      }
    },
  )
}

inline fun ExoPlayer.onPositionDiscontinuity(crossinline action: () -> Unit) {
  addListener(
    object : Player.Listener {
      override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        action()
      }
    },
  )
}

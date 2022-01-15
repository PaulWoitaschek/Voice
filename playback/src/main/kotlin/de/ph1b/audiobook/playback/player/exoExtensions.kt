package de.ph1b.audiobook.playback.player

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import de.ph1b.audiobook.playback.BuildConfig
import de.ph1b.audiobook.playback.playstate.PlayerState

fun ExoPlayer.onSessionPlaybackStateNeedsUpdate(listener: () -> Unit) {
  addListener(object : Player.Listener {
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
      listener()
    }

    override fun onPositionDiscontinuity(reason: Int) {
      listener()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      listener()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
      listener()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
      listener()
    }
  })
}

inline fun ExoPlayer.onStateChanged(crossinline action: (PlayerState) -> Unit) {
  addListener(
    object : Player.Listener {
      override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
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
        if (state != null) action(state)
      }
    }
  )
}

inline fun ExoPlayer.onError(crossinline action: (ExoPlaybackException) -> Unit) {
  addListener(
    object : Player.Listener {
      override fun onPlayerError(error: ExoPlaybackException) {
        action(error)
      }
    }
  )
}

inline fun ExoPlayer.onPositionDiscontinuity(crossinline action: () -> Unit) {
  addListener(
    object : Player.Listener {
      override fun onPositionDiscontinuity(reason: Int) {
        action()
      }
    }
  )
}

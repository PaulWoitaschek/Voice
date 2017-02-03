package de.ph1b.audiobook.playback.utils

import android.media.PlaybackParams
import android.os.Build
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer


inline fun ExoPlayer.onPositionDiscontinuity(crossinline action: () -> Unit) {
  addListener(object : SimpleEventListener {
    override fun onPositionDiscontinuity() {
      action()
    }
  })
}

inline fun ExoPlayer.onStateChanged(crossinline action: (PlayerState) -> Unit) {
  addListener(object : SimpleEventListener {
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      val state = when (playbackState) {
        ExoPlayer.STATE_ENDED -> PlayerState.ENDED
        ExoPlayer.STATE_IDLE -> PlayerState.IDLE
        ExoPlayer.STATE_READY, ExoPlayer.STATE_BUFFERING -> {
          if (playWhenReady) PlayerState.PLAYING
          else PlayerState.PAUSED
        }
        else -> null
      }
      if (state != null) action(state)
    }
  })
}


inline fun ExoPlayer.onError(crossinline action: (ExoPlaybackException) -> Unit) {
  addListener(object : SimpleEventListener {
    override fun onPlayerError(error: ExoPlaybackException) {
      action(error)
    }
  })
}

inline fun SimpleExoPlayer.onAudioSessionId(crossinline action: (Int) -> Unit) {
  setAudioDebugListener(object : SimpleAudioRendererEventListener {
    override fun onAudioSessionId(audioSessionId: Int) {
      action(audioSessionId)
    }
  })
}

fun SimpleExoPlayer.setPlaybackSpeed(speed: Float) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    playbackParams = PlaybackParams().apply {
      this.speed = speed
    }
  }
}


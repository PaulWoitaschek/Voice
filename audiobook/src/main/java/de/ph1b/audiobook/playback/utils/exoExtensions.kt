package de.ph1b.audiobook.playback.utils

import android.media.PlaybackParams
import android.os.Build
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer


inline fun ExoPlayer.onEnded(crossinline action: () -> Unit) {
  addListener(object : SimpleEventListener {
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      if (playbackState == ExoPlayer.STATE_ENDED) action()
    }
  })
}

inline fun ExoPlayer.onPositionDiscontinuity(crossinline action: () -> Unit) {
  addListener(object : SimpleEventListener {
    override fun onPositionDiscontinuity() {
      action()
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


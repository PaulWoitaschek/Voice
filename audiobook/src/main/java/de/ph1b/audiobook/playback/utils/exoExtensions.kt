package de.ph1b.audiobook.playback.utils

import android.media.PlaybackParams
import android.os.Build
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import io.reactivex.Observable


inline fun ExoPlayer.onEnded(crossinline action: () -> Unit) {
  addListener(object : SimpleEventListener {
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      if (playbackState == ExoPlayer.STATE_ENDED) action()
    }
  })
}

fun ExoPlayer.stateChanges(): Observable<PlayerState> = Observable.create<PlayerState> {
  val listener = object : SimpleEventListener {
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      val state = PlayerState.byExoState(playWhenReady, playbackState)
      it.onNext(state)
    }
  }

  addListener(listener)
  it.setCancellable { removeListener(listener) }
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


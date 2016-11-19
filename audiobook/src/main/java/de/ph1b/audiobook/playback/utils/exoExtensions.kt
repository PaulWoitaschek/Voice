package de.ph1b.audiobook.playback.utils

import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters


inline fun ExoPlayer.onEnded(crossinline action: () -> Unit) {
  addListener(object : ExoPlayer.EventListener {
    override fun onPlayerError(error: ExoPlaybackException?) {
    }

    override fun onLoadingChanged(isLoading: Boolean) {
    }

    override fun onPositionDiscontinuity() {
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      if (playbackState == ExoPlayer.STATE_ENDED) action()
    }
  })
}

inline fun ExoPlayer.onError(crossinline action: (ExoPlaybackException?) -> Unit) {
  addListener(object : ExoPlayer.EventListener {
    override fun onPlayerError(error: ExoPlaybackException?) {
      action(error)
    }

    override fun onLoadingChanged(isLoading: Boolean) {
    }

    override fun onPositionDiscontinuity() {
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
    }
  })
}

inline fun SimpleExoPlayer.onAudioSessionId(crossinline action: (Int) -> Unit) {
  setAudioDebugListener(object : AudioRendererEventListener {
    override fun onAudioDecoderInitialized(decoderName: String?, initializedTimestampMs: Long, initializationDurationMs: Long) {
    }

    override fun onAudioEnabled(counters: DecoderCounters?) {
    }

    override fun onAudioInputFormatChanged(format: Format?) {
    }

    override fun onAudioTrackUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
    }

    override fun onAudioSessionId(audioSessionId: Int) {
      action(audioSessionId)
    }

    override fun onAudioDisabled(counters: DecoderCounters?) {
    }
  })
}
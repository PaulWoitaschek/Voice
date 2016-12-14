package de.ph1b.audiobook.playback.utils

import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters

/**
 * Extension that just provides default methods for better readability
 *
 * @author Paul Woitaschek
 */
interface SimpleAudioRendererEventListener : AudioRendererEventListener {
  override fun onAudioDecoderInitialized(decoderName: String?, initializedTimestampMs: Long, initializationDurationMs: Long) {
  }

  override fun onAudioEnabled(counters: DecoderCounters?) {
  }

  override fun onAudioInputFormatChanged(format: Format?) {
  }

  override fun onAudioTrackUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
  }

  override fun onAudioSessionId(audioSessionId: Int) {
  }

  override fun onAudioDisabled(counters: DecoderCounters?) {
  }
}
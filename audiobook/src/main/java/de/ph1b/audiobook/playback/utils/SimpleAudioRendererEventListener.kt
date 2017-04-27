package de.ph1b.audiobook.playback.utils

import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters

/**
 * Empty default callbacks for better readability
 *
 * @author Paul Woitaschek
 */
interface SimpleAudioRendererEventListener : AudioRendererEventListener {
  override fun onAudioEnabled(p0: DecoderCounters?) {}
  override fun onAudioInputFormatChanged(p0: Format?) {}
  override fun onAudioTrackUnderrun(p0: Int, p1: Long, p2: Long) {}
  override fun onAudioSessionId(audioSessionId: Int) {}
  override fun onAudioDecoderInitialized(p0: String?, p1: Long, p2: Long) {}
  override fun onAudioDisabled(p0: DecoderCounters?) {}
}
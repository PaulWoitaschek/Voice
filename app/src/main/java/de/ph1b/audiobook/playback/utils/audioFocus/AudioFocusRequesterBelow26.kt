package de.ph1b.audiobook.playback.utils.audioFocus

import android.media.AudioManager

class AudioFocusRequesterBelow26(
  private val audioFocusListener: AudioManager.OnAudioFocusChangeListener,
  private val audioManager: AudioManager
) : AudioFocusRequester {

  @Suppress("DEPRECATION")
  override fun request() = audioManager.requestAudioFocus(
    audioFocusListener,
    AudioManager.STREAM_MUSIC,
    AudioManager.AUDIOFOCUS_GAIN
  )

  @Suppress("DEPRECATION")
  override fun abandon() {
    audioManager.abandonAudioFocus(audioFocusListener)
  }
}

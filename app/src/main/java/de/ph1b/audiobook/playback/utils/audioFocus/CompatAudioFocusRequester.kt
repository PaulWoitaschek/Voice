package de.ph1b.audiobook.playback.utils.audioFocus

import android.media.AudioManager
import android.os.Build

class CompatAudioFocusRequester(
  private val audioFocusListener: AudioManager.OnAudioFocusChangeListener,
  private val audioManager: AudioManager
) : AudioFocusRequester by impl(audioFocusListener, audioManager)

private fun impl(
  audioFocusListener: AudioManager.OnAudioFocusChangeListener,
  audioManager: AudioManager
): AudioFocusRequester {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    AudioFocusRequesterApi26(audioFocusListener, audioManager)
  } else AudioFocusRequesterBelow26(audioFocusListener, audioManager)
}

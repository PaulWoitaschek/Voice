package de.ph1b.audiobook.playback.utils.audioFocus

import android.annotation.TargetApi
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

@TargetApi(Build.VERSION_CODES.O)
class AudioFocusRequesterApi26(
  audioFocusListener: AudioManager.OnAudioFocusChangeListener,
  private val audioManager: AudioManager
) : AudioFocusRequester {

  private val audioAttributes = AudioAttributes.Builder()
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .build()!!

  private var focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
    .setAudioAttributes(audioAttributes)
    .setWillPauseWhenDucked(true)
    .setAcceptsDelayedFocusGain(false)
    .setOnAudioFocusChangeListener(audioFocusListener)
    .build()!!

  override fun request() = audioManager.requestAudioFocus(focusRequest)

  override fun abandon() {
    audioManager.abandonAudioFocusRequest(focusRequest)
  }
}

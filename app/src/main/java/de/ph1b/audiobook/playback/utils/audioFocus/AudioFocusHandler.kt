package de.ph1b.audiobook.playback.utils.audioFocus

import android.media.AudioManager
import android.telephony.TelephonyManager
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.MediaPlayer
import de.ph1b.audiobook.playback.PlayStateManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import kotlin.properties.Delegates

class AudioFocusHandler @Inject constructor(
  audioManager: AudioManager,
  private val telephonyManager: TelephonyManager,
  private val player: MediaPlayer,
  private val playStateManager: PlayStateManager,
  @Named(PrefKeys.RESUME_AFTER_CALL)
  private val resumeAfterCallPref: Pref<Boolean>
) {

  private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    handleFocusChange(focusChange)
  }

  private val focusRequester = CompatAudioFocusRequester(audioFocusListener, audioManager)

  private var currentlyHasFocus by Delegates.observable(false) { _, _, value ->
    Timber.i("currentlyHasFocus $value")
  }

  @Synchronized
  private fun handleFocusChange(focusChange: Int) {
    Timber.i("handleFocusChange $focusChange")
    currentlyHasFocus = focusChange == AudioManager.AUDIOFOCUS_GAIN
    val callState = telephonyManager.callState
    if (callState != TelephonyManager.CALL_STATE_IDLE) {
      handlePhoneNotIdle(callState)
    } else when (focusChange) {
      AudioManager.AUDIOFOCUS_GAIN -> handleGain()
      AudioManager.AUDIOFOCUS_LOSS -> handleLoss()
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> handleTemporaryLoss()
      else -> Timber.d("ignore audioFocus=$focusChange")
    }
  }

  private fun handlePhoneNotIdle(callState: Int) {
    Timber.d("Call state is $callState. Pausing now")
    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.PLAYING
    player.pause(true)
    playStateManager.pauseReason =
        if (wasPlaying) PlayStateManager.PauseReason.CALL else PlayStateManager.PauseReason.NONE
  }

  private fun handleTemporaryLoss() {
    if (playStateManager.playState == PlayStateManager.PlayState.PLAYING) {
      Timber.d("Paused by audio-focus loss transient.")
      player.pause(rewind = false)
      playStateManager.pauseReason = PlayStateManager.PauseReason.LOSS_TRANSIENT
    }
  }

  private fun handleLoss() {
    Timber.d("paused by audioFocus loss")
    player.pause(rewind = true)
    playStateManager.pauseReason = PlayStateManager.PauseReason.NONE
  }

  private fun handleGain() {
    Timber.d("gain")
    val pauseReason = playStateManager.pauseReason
    if (pauseReason == PlayStateManager.PauseReason.LOSS_TRANSIENT) {
      Timber.d("loss was transient so start playback")
      player.play()
    } else if (pauseReason == PlayStateManager.PauseReason.CALL && resumeAfterCallPref.value) {
      Timber.d("we were paused because of a call and we should resume after a call. Start playback")
      player.play()
    }
  }

  @Synchronized
  fun request() {
    Timber.i("request")
    if (currentlyHasFocus) {
      Timber.d("has focus already")
      return
    }

    val result = focusRequester.request()
    currentlyHasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
  }

  @Synchronized
  fun abandon() {
    Timber.i("abandon")
    if (!currentlyHasFocus) {
      Timber.d("does not have focus.")
      return
    }

    focusRequester.abandon()
    currentlyHasFocus = false
  }
}

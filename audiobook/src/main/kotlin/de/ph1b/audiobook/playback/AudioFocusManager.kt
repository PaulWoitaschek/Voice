package de.ph1b.audiobook.playback

import android.media.AudioManager
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.receiver.AudioFocus
import rx.Observable
import rx.Subscription
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls playback based on changing audio focus.
 *
 * @author Paul Woitaschek
 */
@Singleton
class AudioFocusManager
@Inject
constructor(private val mediaPlayerController: MediaPlayerController, private val playStateManager: PlayStateManager, private val audioManager: AudioManager, private val prefsManager: PrefsManager) {

    fun handleAudioFocus(audioFocusObservable: Observable<AudioFocus>): Subscription =
            audioFocusObservable.subscribe { audioFocus: AudioFocus ->
                when (audioFocus) {
                    AudioFocus.GAIN -> {
                        Timber.d("started by audioFocus gained")
                        if (playStateManager.pauseReason == PlayStateManager.PauseReason.LOSS_TRANSIENT) {
                            mediaPlayerController.play()
                        } else if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            Timber.d("increasing volume")
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                        }
                    }
                    AudioFocus.LOSS,
                    AudioFocus.LOSS_INCOMING_CALL -> {
                        Timber.d("paused by audioFocus loss")
                        mediaPlayerController.stop()
                    }
                    AudioFocus.LOSS_TRANSIENT_CAN_DUCK -> {
                        if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            if (prefsManager.pauseOnTempFocusLoss()) {
                                Timber.d("Paused by audio-focus loss transient.")
                                // Pause is temporary, don't rewind
                                mediaPlayerController.pause(false)
                                playStateManager.pauseReason = PlayStateManager.PauseReason.LOSS_TRANSIENT
                            } else {
                                Timber.d("lowering volume")
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                            }
                        }
                    }
                    AudioFocus.LOSS_TRANSIENT -> {
                        if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            Timber.d("Paused by audio-focus loss transient.")
                            mediaPlayerController.pause(true) // auto pause
                            playStateManager.pauseReason = PlayStateManager.PauseReason.LOSS_TRANSIENT
                        }
                    }
                }
            }
}
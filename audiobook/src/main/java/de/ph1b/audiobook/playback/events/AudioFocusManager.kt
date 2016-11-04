package de.ph1b.audiobook.playback.events

import d
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import i
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
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
constructor(private val mediaPlayer: PlayerController, private val playStateManager: PlayStateManager, private val prefsManager: PrefsManager) {

    fun handleAudioFocus(audioFocusObservable: Observable<AudioFocus>): Disposable =
            audioFocusObservable.subscribe { audioFocus: AudioFocus ->
                i { "handleAudioFocu changed to $audioFocus" }
                when (audioFocus) {
                    AudioFocus.GAIN -> {
                        d { "started by audioFocus gained" }
                        if (playStateManager.pauseReason == PlayStateManager.PauseReason.LOSS_TRANSIENT) {
                            mediaPlayer.play()
                        } else if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            d { "increasing volume" }
                            mediaPlayer.volume(loud = true)
                        }
                    }
                    AudioFocus.LOSS,
                    AudioFocus.LOSS_INCOMING_CALL -> {
                        d { "paused by audioFocus loss" }
                        mediaPlayer.stop()
                    }
                    AudioFocus.LOSS_TRANSIENT_CAN_DUCK -> {
                        if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            if (prefsManager.pauseOnTempFocusLoss.value()) {
                                d { "Paused by audio-focus loss transient." }
                                // Pause is temporary, don't rewind
                                mediaPlayer.pauseNonRewinding()
                                playStateManager.pauseReason = PlayStateManager.PauseReason.LOSS_TRANSIENT
                            } else {
                                d { "lowering volume" }
                                mediaPlayer.volume(loud = false)
                            }
                        }
                    }
                    AudioFocus.LOSS_TRANSIENT -> {
                        if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            d { "Paused by audio-focus loss transient." }
                            mediaPlayer.pause() // auto pause
                            playStateManager.pauseReason = PlayStateManager.PauseReason.LOSS_TRANSIENT
                        }
                    }
                }
            }
}
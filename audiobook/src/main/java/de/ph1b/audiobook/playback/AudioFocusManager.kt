/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.playback

import Slimber
import android.media.AudioManager
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.receiver.AudioFocus
import rx.Observable
import rx.Subscription
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
constructor(private val mediaPlayer: PlayerController, private val playStateManager: PlayStateManager, private val audioManager: AudioManager, private val prefsManager: PrefsManager) {

    fun handleAudioFocus(audioFocusObservable: Observable<AudioFocus>): Subscription =
            audioFocusObservable.subscribe { audioFocus: AudioFocus ->
                Slimber.i { "handleAudioFocu changed to $audioFocus" }
                when (audioFocus) {
                    AudioFocus.GAIN -> {
                        Slimber.d { "started by audioFocus gained" }
                        if (playStateManager.pauseReason == PlayStateManager.PauseReason.LOSS_TRANSIENT) {
                            mediaPlayer.play()
                        } else if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            Slimber.d { "increasing volume" }
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                        }
                    }
                    AudioFocus.LOSS,
                    AudioFocus.LOSS_INCOMING_CALL -> {
                        Slimber.d { "paused by audioFocus loss" }
                        mediaPlayer.stop()
                    }
                    AudioFocus.LOSS_TRANSIENT_CAN_DUCK -> {
                        if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            if (prefsManager.pauseOnTempFocusLoss()) {
                                Slimber.d { "Paused by audio-focus loss transient." }
                                // Pause is temporary, don't rewind
                                mediaPlayer.pauseNonRewinding()
                                playStateManager.pauseReason = PlayStateManager.PauseReason.LOSS_TRANSIENT
                            } else {
                                Slimber.d { "lowering volume" }
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                            }
                        }
                    }
                    AudioFocus.LOSS_TRANSIENT -> {
                        if (playStateManager.playState.value === PlayStateManager.PlayState.PLAYING) {
                            Slimber.d { "Paused by audio-focus loss transient." }
                            mediaPlayer.pause() // auto pause
                            playStateManager.pauseReason = PlayStateManager.PauseReason.LOSS_TRANSIENT
                        }
                    }
                }
            }
}
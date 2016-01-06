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

import android.content.Context
import rx.subjects.PublishSubject

/**
 * Delegates to antenna player.
 *
 * @author Paul Woiitaschek
 */
class AntennaPlayerDelegate(context: Context) : MediaPlayer {

    val player = object : org.antennapod.audio.MediaPlayer(context, false) {
        override fun useSonic() = true
    }

    init {
        player.setOnErrorListener { mediaPlayer, i, j ->
            errorSubject.onNext(Unit)
            false
        }
        player.setOnCompletionListener { completionSubject.onNext(Unit) }
    }

    override fun setDataSource(path: String) {
        player.setDataSource(path)
    }

    override fun seekTo(to: Int) {
        player.seekTo(to)
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    override fun start() {
        player.start()
    }

    override fun pause() {
        player.pause()
    }

    override fun reset() {
        player.reset()
    }

    override fun setWakeMode(context: Context, mode: Int) {
        player.setWakeMode(context, mode)
    }

    override fun setAudioStreamType(streamType: Int) {
        player.setAudioStreamType(streamType)
    }

    override val currentPosition: Int
        get() = player.currentPosition
    override val duration: Int
        get() = player.duration
    override var playbackSpeed: Float
        get() = player.currentSpeedMultiplier
        set(value) {
            player.setPlaybackSpeed(value)
        }

    override fun prepare() {
        player.prepare()
    }

    private val errorSubject = PublishSubject.create<Unit>()

    override val onError = errorSubject.asObservable()

    private val completionSubject = PublishSubject.create<Unit>()

    override val onCompletion = completionSubject.asObservable()
}
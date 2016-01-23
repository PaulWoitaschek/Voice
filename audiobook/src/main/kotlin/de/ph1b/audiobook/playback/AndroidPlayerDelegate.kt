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
 * Delegates to android.media.MediaPlayer.
 *
 * @author Paul Woitaschek
 */
class AndroidPlayerDelegate : MediaPlayer {

    private val player = android.media.MediaPlayer()

    init {
        player.setOnErrorListener { mediaPlayer, i, j ->
            errorSubject.onNext(Unit)
            false
        }
        player.setOnCompletionListener { completionSubject.onNext(Unit) }
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

    override fun setWakeMode(context: Context, mode: Int) {
        player.setWakeMode(context, mode)
    }

    override fun setAudioStreamType(streamType: Int) {
        player.setAudioStreamType(streamType)
    }

    override val duration: Int
        get() = player.duration

    override var playbackSpeed: Float = 1F

    override fun setDataSource(path: String) {
        player.setDataSource(path)
    }

    override fun prepare() {
        player.prepare()
    }

    override fun reset() {
        player.reset()
    }

    override val currentPosition: Int
        get() = player.currentPosition

    private val errorSubject = PublishSubject.create<Unit>()

    override val onError = errorSubject.asObservable()

    private val completionSubject = PublishSubject.create<Unit>()

    override val onCompletion = completionSubject.asObservable()
}
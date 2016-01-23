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
import rx.Observable

/**
 * Abstraction of android.media.MediaPlayer.
 *
 * @author Paul Woitaschek
 */
interface MediaPlayer {

    fun setDataSource(path: String)

    fun seekTo(to: Int)

    fun isPlaying(): Boolean

    fun prepare()

    fun start()

    fun pause()

    fun reset()

    fun setWakeMode(context: Context, mode: Int)

    fun setAudioStreamType(streamType: Int)

    val currentPosition: Int

    val duration: Int

    var playbackSpeed: Float

    val onError: Observable<Unit>

    val onCompletion: Observable<Unit>
}
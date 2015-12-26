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

package de.ph1b.audiobook.mediaplayer

import rx.Observable
import java.io.File

/**
 * Interface defining a media player.
 *
 * @author Paul Woitaschek
 */
interface Player {


    /**
     * Prepares an audio file.
     */
    fun prepare(file: File)

    /**
     * The current position in the track.
     */
    var currentPosition: Int

    /**
     * If true the player will start as soon as he is prepared
     */
    var playing: Boolean

    /**
     * The playback rate. 1.0 is normal
     */
    var playbackSpeed: Float


    /**
     * An observable that emits when an error is detected
     */
    val errorObservable: Observable<Unit>


    /**
     * An observable that emits once a track is finished.
     */
    val completionObservable: Observable<Unit>

    /**
     * The duration of the current track
     */
    val duration: Int
}
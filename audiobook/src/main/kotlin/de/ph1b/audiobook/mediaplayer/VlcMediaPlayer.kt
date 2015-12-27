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

import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import rx.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Wraps [LibVLC] in a convenient interface.
 *
 * @author Paul Woitschek
 */
class VlcMediaPlayer
@Inject constructor()
: Player {

    private val vlc = LibVLC()
    private val player = MediaPlayer(vlc)

    init {
        player.setEventListener({
            when (it?.type) {
                MediaPlayer.Event.EncounteredError -> errorSubject.onNext(Unit)
                MediaPlayer.Event.EndReached -> completionSubject.onNext(Unit)
            }
        })
    }


    override fun prepare(file: File) {
        Timber.i("prepare $file")
        val media = Media(vlc, file.absolutePath)
        player.media = media

        // necessary to play-pause. Else duration etc will return -1
        player.play()
        player.pause()
    }

    override var currentPosition: Int
        get() = player.time.toInt()
        set(value) {
            player.setTime(value.toLong())
        }

    override var playing: Boolean
        get() = player.isPlaying
        set(value) {
            if (value)
                player.play()
            else
                player.pause()
        }

    override var playbackSpeed: Float
        get() = player.rate
        set(value) {
            player.rate = value
        }

    private val errorSubject = PublishSubject.create<Unit>()
    private val completionSubject = PublishSubject.create<Unit>()

    override val errorObservable = errorSubject.asObservable()

    override val completionObservable = completionSubject.asObservable()

    override val duration: Int
        get() = player.length.toInt()
}
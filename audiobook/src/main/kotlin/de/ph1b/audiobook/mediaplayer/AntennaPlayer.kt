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

import android.content.Context
import android.os.Build
import org.antennapod.audio.MediaPlayer
import rx.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Wrapper around AntennaPods player.
 *
 * @author Paul Woitaschek
 */
class AntennaPlayer
@Inject
constructor(context: Context)
: Player {

    private var state = State.NONE

    private val mediaPlayer = object : MediaPlayer(context, false) {
        override fun useSonic() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
    }

    init {
        mediaPlayer.setOnErrorListener { mediaPlayer, i, j ->
            mediaPlayer.reset()
            state = State.NONE
            errorSubject.onNext(Unit)
            true
        }

        mediaPlayer.setOnCompletionListener {
            if (currentFile != null) prepare(currentFile!!)
            completionSubject.onNext(Unit)
        }
    }

    private var currentFile: File? = null

    override fun prepare(file: File) {
        currentFile = file
        mediaPlayer.reset()
        mediaPlayer.setDataSource(file.absolutePath)
        mediaPlayer.prepare()
        state = State.PREPARED
    }

    override var currentPosition: Int
        get() = when (state) {
            State.PREPARED, State.PLAYING -> mediaPlayer.currentPosition
            else -> 0
        }
        set(value) {
            when (state) {
                State.PREPARED, State.PLAYING -> {
                    mediaPlayer.seekTo(value)
                }
                else -> Timber.e("Get current position called in state $state")
            }
        }

    override var playing: Boolean
        get() = mediaPlayer.isPlaying
        set(value) {
            if (value) {
                when (state) {
                    State.PREPARED, State.PLAYING -> {
                        mediaPlayer.start()
                        state = State.PLAYING
                    }
                    else -> Timber.e("Play called in state $state")
                }
            } else {
                when (state) {
                    State.PLAYING -> {
                        mediaPlayer.pause()
                        state = State.PREPARED
                    }
                    else -> Timber.e("Pause called in state $state")
                }
            }
        }

    override var playbackSpeed: Float
        get() = mediaPlayer.currentSpeedMultiplier
        set(value) {
            mediaPlayer.setPlaybackSpeed(value)
        }

    private val errorSubject = PublishSubject.create<Unit>()

    private val completionSubject = PublishSubject.create<Unit>()

    override val errorObservable = errorSubject.asObservable()

    override val completionObservable = completionSubject.asObservable()

    override val duration: Int
        get() = when (state) {
            State.PREPARED, State.PLAYING -> mediaPlayer.duration
            else -> 0
        }

    private enum class State {
        NONE,
        PREPARED,
        PLAYING
    }
}
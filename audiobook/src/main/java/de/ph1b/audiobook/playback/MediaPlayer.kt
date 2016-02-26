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
import Slimber.e
import de.ph1b.audiobook.assertMain
import de.ph1b.audiobook.model.Book
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import de.paul_woitaschek.mediaplayer.MediaPlayer as InternalPlayer

@Singleton
class MediaPlayer
@Inject
constructor(private val player: InternalPlayer, private val playStateManager: PlayStateManager) {

    private var book = BehaviorSubject.create<Book>()
    private var state = BehaviorSubject.create(State.IDLE)

    private val subscriptions = CompositeSubscription()
    private val errorSubject = PublishSubject.create<Unit>()
    fun onError() = errorSubject.asObservable()

    init {
        player.onCompletion
                .subscribe {
                    // After the current song has ended, prepare the next one if there is one. Else stop the
                    // resources.
                    book.value?.let {
                        Slimber.v { "onCompletion called, nextChapter=${it.nextChapter()}" }
                        if (it.nextChapter() != null) {
                            next()
                        } else {
                            Slimber.v { "Reached last track. Stopping player" }
                            stopUpdating()
                            playStateManager.playState.onNext(PlayStateManager.PlayState.STOPPED)

                            state.onNext(State.PLAYBACK_COMPLETED)
                        }
                    }
                }

        player.onError
                .subscribe {
                    player.reset()
                    state.onNext(MediaPlayer.State.IDLE)
                    errorSubject.onNext(Unit)
                }
    }


    /**
     * Initializes a new book. After this, a call to play can be made.

     * @param book The book to be initialized.
     */
    fun init(book: Book) {
        assertMain()

        if (this.book.value != book) {
            Slimber.i { "constructor called with ${book.name}" }
            this.book.onNext(book)
        }
    }

    fun book() = book.value

    fun bookObservable() = book.asObservable()

    /**
     * Prepares the current chapter set in book.
     */
    private fun prepare() {
        assertMain()

        book.value?.let {
            try {
                if (state.value != State.IDLE) player.reset()

                player.prepare(it.currentChapter().file)
                player.seekTo(it.time)
                player.playbackSpeed = it.playbackSpeed
                state.onNext(State.PAUSED)
            } catch (ex: IOException) {
                e(ex) { "Error when preparing the player." }
                state.onNext(State.STOPPED)
            }
        }
    }


    /**
     * Plays the prepared file.
     */
    fun play() {
        assertMain()

        when (state.value) {
            State.PLAYBACK_COMPLETED -> {
                player.seekTo(0)
                player.start()
                startUpdating()
                playStateManager.playState.onNext(PlayStateManager.PlayState.PLAYING)
                state.onNext(State.STARTED)
            }
            State.PAUSED -> {
                player.start()
                startUpdating()
                playStateManager.playState.onNext(PlayStateManager.PlayState.PLAYING)
                state.onNext(State.STARTED)
            }
            State.STOPPED -> {
                prepare()
                if (state.value == State.PAUSED) {
                    play()
                }
            }
            else -> Slimber.i { "Play ignores state=$state " }
        }
    }

    /**
     * Updates the current time and position of the book, writes it to the database and sends
     * updates to the GUI.
     */
    private fun startUpdating() {
        Slimber.v { "startUpdating" }
        subscriptions.apply {
            if (!hasSubscriptions()) {
                // updates the book automatically with the current position
                add(Observable.interval(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                        .map { if (state.value == State.STARTED) player.currentPosition else -1 }
                        .filter { it != -1 }
                        .distinctUntilChanged()
                        .map { book.value?.copy(time = it) } // create a copy with new position
                        .filter { it != null } // let it pass when it exists
                        .subscribe { book.onNext(it) } // update the book
                )
            }
        }
    }

    var seekTime = 0
    var autoRewindAmount = 0

    /**
     * Skips by the amount, specified in the settings.

     * @param direction The direction to skip
     */
    fun skip(direction: Direction) {
        assertMain()

        Slimber.v { "direction=$direction" }
        book.value?.let {
            if (state.value == State.IDLE && state.value == State.STOPPED) {
                prepare()
                if (state.value != State.PREPARED) return
            }

            val currentPos = player.currentPosition
            val duration = player.duration
            val delta = seekTime * 1000

            val seekTo = if ((direction == Direction.FORWARD)) currentPos + delta else currentPos - delta
            Slimber.v { "currentPos=$currentPos, seekTo=$seekTo, duration=$duration" }

            if (seekTo < 0) {
                previous(false)
            } else if (seekTo > duration) {
                next()
            } else {
                changePosition(seekTo, it.currentFile)
            }
        }
    }

    /**
     * If current time is > 2000ms, seek to 0. Else play previous chapter if there is one.
     */
    fun previous(toNullOfNewTrack: Boolean) {
        assertMain()

        book.value?.let {
            if (state.value == State.IDLE || state.value == State.STOPPED) {
                prepare()
                if (state.value != State.PREPARED) return
            }

            val previousChapter = it.previousChapter()
            if (player.currentPosition > 2000 || previousChapter == null) {
                player.seekTo(0)
                val copy = it.copy(time = 0)
                book.onNext(copy)
            } else {
                if (toNullOfNewTrack) {
                    changePosition(0, previousChapter.file)
                } else {
                    changePosition(previousChapter.duration - (seekTime * 1000), previousChapter.file)
                }
            }
        }
    }

    /**
     * Stops the playback and releases some resources.
     */
    fun stop() {
        assertMain()

        if (state.value == State.STARTED) player.pause()
        stopUpdating()
        playStateManager.playState.onNext(PlayStateManager.PlayState.STOPPED)
        state.onNext(State.STOPPED)
    }

    /**
     * Stops updating the book with the current position.
     */
    private fun stopUpdating() {
        subscriptions.clear()
    }


    /**
     * Pauses the player. Also stops the updating mechanism which constantly updates the book to the
     * database.

     * @param rewind true if the player should automatically rewind a little bit.
     */
    fun pause(rewind: Boolean) {
        assertMain()

        Slimber.v { "pause acquired lock. state is=$state" }
        book.value?.let {
            when (state.value) {
                State.STARTED -> {
                    player.pause()
                    stopUpdating()

                    if (rewind) {
                        val autoRewind = autoRewindAmount * 1000
                        if (autoRewind != 0) {
                            val originalPosition = player.currentPosition
                            var seekTo = originalPosition - autoRewind
                            seekTo = Math.max(seekTo, 0)
                            player.seekTo(seekTo)
                            val copy = it.copy(time = seekTo)
                            book.onNext(copy)
                        }
                    }

                    playStateManager.playState.onNext(PlayStateManager.PlayState.PAUSED)

                    state.onNext(State.PAUSED)
                }
                else -> Slimber.e { "pause called in illegal state=$state" }
            }
        }
    }

    fun playPause() {
        assertMain()

        if (playStateManager.playState.value != PlayStateManager.PlayState.PLAYING) {
            play()
        } else {
            pause(true)
        }
    }


    /**
     * Plays the next chapter. If there is none, don't do anything.
     */
    fun next() {
        assertMain()

        book.value?.nextChapter()?.let {
            changePosition(0, it.file)
        }
    }

    /**
     * Changes the current position in book. If the path is the same, continues playing the song.
     * Else calls [.prepare] to prepare the next file

     * @param time The time in chapter at which to start
     * *
     * @param file The path of the media to play (relative to the books root path)
     */
    fun changePosition(time: Int, file: File) {
        assertMain()

        Slimber.v { "changePosition with time $time and file $file" }
        book.value?.let {
            val changeFile = (it.currentChapter().file != file)
            Slimber.v { "changeFile=$changeFile" }
            if (changeFile) {
                val wasPlaying = (state.value == State.STARTED)

                val copy = it.copy(currentFile = file, time = time)
                book.onNext(copy)

                prepare()
                if (wasPlaying) {
                    player.start()
                    state.onNext(State.STARTED)
                    playStateManager.playState.onNext(PlayStateManager.PlayState.PLAYING)
                } else {
                    playStateManager.playState.onNext(PlayStateManager.PlayState.PAUSED)
                }
            } else {
                if (state.value == State.STOPPED || state.value == State.IDLE) prepare()
                when (state.value) {
                    State.STARTED, State.PAUSED -> {
                        player.seekTo(time)

                        val copy = it.copy(time = time)
                        book.onNext(copy)
                    }
                    else -> Slimber.e { "changePosition called in illegal state=$state" }
                }
            }
        }
    }

    /**
     * The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc.
     */
    fun setPlaybackSpeed(speed: Float) {
        assertMain()

        book.value?.let {
            val copy = it.copy(playbackSpeed = speed)
            book.onNext(copy)

            player.playbackSpeed = speed
        }
    }

    /**
     * The direction to skip.
     */
    enum class Direction {
        FORWARD, BACKWARD
    }

    /**
     * The various internal states the player can have.
     */
    private enum class State {
        IDLE,
        PREPARED,
        STARTED,
        PAUSED,
        STOPPED,
        PLAYBACK_COMPLETED;
    }
}

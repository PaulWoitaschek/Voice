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
import de.paul_woitaschek.mediaplayer.Player
import de.ph1b.audiobook.model.Book
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.io.File
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class MediaPlayer
@Inject
constructor(private val player: Player, private val playStateManager: PlayStateManager) {

    private val lock = ReentrantLock()
    private var book = BehaviorSubject.create<Book>()
    @Volatile private var state: State = State.STOPPED

    private val subscriptions = CompositeSubscription()
    private val errorSubject = PublishSubject.create<Unit>()
    fun onError() = errorSubject.asObservable()

    init {
        player.completionObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // After the current song has ended, prepare the next one if there is one. Else stop the
                    // resources.
                    lock.withLock {
                        book.value?.let {
                            Slimber.v { "onCompletion called, nextChapter=${it.nextChapter()}" }
                            if (it.nextChapter() != null) {
                                next()
                            } else {
                                Slimber.v { "Reached last track. Stopping player" }
                                stopUpdating()
                                playStateManager.playState.onNext(PlayStateManager.PlayState.STOPPED)

                                state = State.COMPLETED
                            }
                        }
                    }
                }

        player.errorObservable
                .subscribe {
                    state = MediaPlayer.State.STOPPED
                    errorSubject.onNext(Unit)
                }
    }


    /**
     * Initializes a new book. After this, a call to play can be made.

     * @param book The book to be initialized.
     */
    fun init(book: Book) {
        lock.withLock {
            if (this.book.value != book) {
                Slimber.i { "constructor called with ${book.name}" }
                this.book.onNext(book)
            }
        }
    }

    fun book() = book.value

    fun bookObservable() = book.asObservable()

    /**
     * Prepares the current chapter set in book.
     */
    private fun prepare() {
        lock.withLock {
            book.value?.let {
                try {
                    player.prepare(it.currentChapter().file)
                    player.currentPosition = it.time
                    player.playbackSpeed = it.playbackSpeed
                    state = State.PAUSED
                } catch (ex: IOException) {
                    e(ex) { "Error when preparing the player." }
                    state = State.STOPPED
                }
            }
        }
    }


    /**
     * Plays the prepared file.
     */
    fun play() {
        lock.withLock {
            when (state) {
                State.COMPLETED -> {
                    player.currentPosition = 0
                    player.playing = true
                    startUpdating()
                    playStateManager.playState.onNext(PlayStateManager.PlayState.PLAYING)
                    state = State.STARTED
                }
                State.PAUSED -> {
                    player.playing = true
                    startUpdating()
                    playStateManager.playState.onNext(PlayStateManager.PlayState.PLAYING)
                    state = State.STARTED
                }
                State.STOPPED -> {
                    prepare()
                    if (state == State.PAUSED) {
                        play()
                    }
                }
                State.STARTED -> {

                }
            }
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
                add(Observable.interval(1, TimeUnit.SECONDS)
                        .map { lock.withLock { player.currentPosition } } // pass the current position
                        .map { lock.withLock { book.value?.copy(time = it) } } // create a copy with new position
                        .filter { it != null } // let it pass when it exists
                        .subscribe { lock.withLock { book.onNext(it) } } // update the book
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
        Slimber.v { "direction=$direction" }
        lock.withLock {
            book.value?.let {
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
    }

    /**
     * If current time is > 2000ms, seek to 0. Else play previous chapter if there is one.
     */
    fun previous(toNullOfNewTrack: Boolean) {
        lock.withLock {
            book.value?.let {
                val previousChapter = it.previousChapter()
                if (player.currentPosition > 2000 || previousChapter == null) {
                    player.currentPosition = 0
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
    }

    /**
     * Stops the playback and releases some resources.
     */
    fun stop() {
        lock.withLock {
            player.playing = false
            stopUpdating()
            playStateManager.playState.onNext(PlayStateManager.PlayState.STOPPED)
            state = State.STOPPED
        }
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
        lock.withLock {
            Slimber.v { "pause acquired lock. state is=$state" }
            book.value?.let {
                when (state) {
                    State.STARTED -> {
                        player.playing = false
                        stopUpdating()

                        if (rewind) {
                            val autoRewind = autoRewindAmount * 1000
                            if (autoRewind != 0) {
                                val originalPosition = player.currentPosition
                                var seekTo = originalPosition - autoRewind
                                seekTo = Math.max(seekTo, 0)
                                player.currentPosition = seekTo
                                val copy = it.copy(time = seekTo)
                                book.onNext(copy)
                            }
                        }

                        playStateManager.playState.onNext(PlayStateManager.PlayState.PAUSED)

                        state = State.PAUSED
                    }
                    else -> Slimber.e { "pause called in illegal state=$state" }
                }
            }
        }
    }

    fun playPause() {
        lock.withLock {
            if (playStateManager.playState.value != PlayStateManager.PlayState.PLAYING) {
                play()
            } else {
                pause(true)
            }
        }
    }


    /**
     * Plays the next chapter. If there is none, don't do anything.
     */
    operator fun next() {
        lock.withLock {
            book.value?.nextChapter()?.let {
                changePosition(0, it.file)
            }
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
        lock.withLock {
            Slimber.v { "changePosition with time $time and file $file" }
            book.value?.let {

                val changeFile = (it.currentChapter().file != file)
                Slimber.v { "changeFile=$changeFile" }
                if (changeFile) {
                    val wasPlaying = (state == State.STARTED)

                    val copy = it.copy(currentFile = file, time = time)
                    book.onNext(copy)

                    prepare()
                    if (wasPlaying) {
                        player.playing = true
                        state = State.STARTED
                        playStateManager.playState.onNext(PlayStateManager.PlayState.PLAYING)
                    } else {
                        state = State.PAUSED
                        playStateManager.playState.onNext(PlayStateManager.PlayState.PAUSED)
                    }
                } else {
                    if (state == State.STOPPED) prepare()
                    when (state) {
                        State.STARTED, State.PAUSED, State.COMPLETED -> {
                            player.currentPosition = time

                            val copy = it.copy(time = time)
                            book.onNext(copy)
                        }
                        else -> Slimber.e { "changePosition called in illegal state=$state" }
                    }
                }
            }
        }
    }

    /**
     * The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc.
     */
    fun setPlaybackSpeed(speed: Float) {
        lock.withLock {
            book.value?.let {
                val copy = it.copy(playbackSpeed = speed)
                book.onNext(copy)

                player.playbackSpeed = speed
            }
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
        PAUSED,
        STARTED,
        STOPPED,
        COMPLETED
    }

    companion object {
        val playerExecutor = ThreadPoolExecutor(
                1, 1, // single thread
                2, TimeUnit.SECONDS,
                LinkedBlockingQueue(2), // queue capacity
                ThreadPoolExecutor.DiscardOldestPolicy()
        )
        val playerScheduler = Schedulers.from(playerExecutor)
    }
}

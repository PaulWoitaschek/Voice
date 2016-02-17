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
import android.content.Intent
import de.paul_woitaschek.mediaplayer.Player
import de.ph1b.audiobook.activity.BookActivity
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.view.fragment.BookShelfFragment
import e
import i
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import v
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class MediaPlayer
@Inject
constructor(private val c: Context, private val prefs: PrefsManager, private val db: BookChest, private val player: Player, private val playStateManager: PlayStateManager) {

    private val lock = ReentrantLock()
    var book: Book? = null
        private set
    @Volatile private var state: State = State.STOPPED

    private val subscriptions = CompositeSubscription()


    init {
        player.completionObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // After the current song has ended, prepare the next one if there is one. Else stop the
                    // resources.
                    lock.withLock {
                        book?.let {
                            v { "onCompletion called, nextChapter=${it.nextChapter()}" }
                            if (it.nextChapter() != null) {
                                next()
                            } else {
                                v { "Reached last track. Stopping player" }
                                stopUpdating()
                                playStateManager.playState.onNext(PlayStateManager.PlayState.STOPPED)

                                state = State.COMPLETED
                            }
                        }
                    }
                }

        player.errorObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // inform user on errors
                    lock.withLock {
                        e { "onError" }
                        if (book != null) {
                            c.startActivity(BookActivity.malformedFileIntent(c, book!!.currentFile))
                        } else {
                            val intent = Intent(c, BookShelfFragment::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            c.startActivity(intent)
                        }

                        state = State.STOPPED
                    }
                }
    }


    /**
     * Initializes a new book. After this, a call to play can be made.

     * @param book The book to be initialized.
     */
    fun init(book: Book) {
        lock.withLock {
            i { "constructor called with ${book.name}" }
            this.book = book
        }
    }

    /**
     * Prepares the current chapter set in book.
     */
    private fun prepare() {
        lock.withLock {
            book?.let {
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
        v { "startUpdating" }
        subscriptions.apply {
            if (!hasSubscriptions()) {
                // updates the book automatically with the current position
                add(Observable.interval(1, TimeUnit.SECONDS)
                        .map { lock.withLock { player.currentPosition } } // pass the current position
                        .map { lock.withLock { book?.copy(time = it) } } // create a copy with new position
                        .filter { it != null } // let it pass when it exists
                        .doOnNext { lock.withLock { book = it } } // update local var
                        .subscribe { lock.withLock { db.updateBook(it!!) } } // update the book
                )
            }
        }
    }

    /**
     * Skips by the amount, specified in the settings.

     * @param direction The direction to skip
     */
    fun skip(direction: Direction) {
        v { "direction=$direction" }
        lock.withLock {
            book?.let {
                val currentPos = player.currentPosition
                val duration = player.duration
                val delta = prefs.seekTime * 1000

                val seekTo = if ((direction == Direction.FORWARD)) currentPos + delta else currentPos - delta
                v { "currentPos=$currentPos, seekTo=$seekTo, duration=$duration" }

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
            book?.let {
                val previousChapter = it.previousChapter()
                if (player.currentPosition > 2000 || previousChapter == null) {
                    player.currentPosition = 0
                    val copy = it.copy(time = 0)
                    book = copy
                    db.updateBook(copy)
                } else {
                    if (toNullOfNewTrack) {
                        changePosition(0, previousChapter.file)
                    } else {
                        changePosition(previousChapter.duration - (prefs.seekTime * 1000), previousChapter.file)
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
            v { "pause acquired lock. state is=$state" }
            book?.let {
                when (state) {
                    State.STARTED -> {
                        player.playing = false
                        stopUpdating()

                        if (rewind) {
                            val autoRewind = prefs.autoRewindAmount * 1000
                            if (autoRewind != 0) {
                                val originalPosition = player.currentPosition
                                var seekTo = originalPosition - autoRewind
                                seekTo = Math.max(seekTo, 0)
                                player.currentPosition = seekTo
                                val copy = it.copy(time = seekTo)
                                db.updateBook(copy)
                                book = copy
                            }
                        }

                        playStateManager.playState.onNext(PlayStateManager.PlayState.PAUSED)

                        state = State.PAUSED
                    }
                    else -> e { "pause called in illegal state=$state" }
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
            book?.nextChapter()?.let {
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
            v { "changePosition with time $time and file $file" }
            book?.let {

                val changeFile = (it.currentChapter().file != file)
                v { "changeFile=$changeFile" }
                if (changeFile) {
                    val wasPlaying = (state == State.STARTED)

                    val copy = it.copy(currentFile = file, time = time)
                    db.updateBook(copy)
                    book = copy

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
                            db.updateBook(copy)
                            book = copy
                        }
                        else -> e { "changePosition called in illegal state=$state" }
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
            book?.let {
                val copy = it.copy(playbackSpeed = speed)
                db.updateBook(copy)
                book = copy

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
}

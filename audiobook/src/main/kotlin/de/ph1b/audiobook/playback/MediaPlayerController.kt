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
import de.ph1b.audiobook.activity.BookActivity
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.view.fragment.BookShelfFragment
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class MediaPlayerController
@Inject
constructor(private val c: Context, private val prefs: PrefsManager, private val db: BookChest,
            private val player: Player, private val playStateManager: PlayStateManager) {

    private val lock = ReentrantLock()
    var book: Book? = null
        private set
    @Volatile private var state: State = State.STOPPED

    private val subscriptions = CompositeSubscription()


    /**
     * The time left till the playback stops in ms. If this is -1 the timer was stopped manually.
     * If this is 0 the timer simple counted down.
     */
    private val internalSleepSand = BehaviorSubject.create<Long>(-1L)

    /**
     * This observable holds the time in ms left that the sleep timer has left. This is updated
     * periodically
     */
    val sleepSand = internalSleepSand.asObservable()

    fun sleepTimerActive(): Boolean = lock.withLock { internalSleepSand.value > 0 }

    init {
        // stops the player when the timer reaches 0
        internalSleepSand.filter { it == 0L } // when this reaches 0
                .subscribe { stop() } // stop the player
        player.completionObservable
                .subscribe {
                    // After the current song has ended, prepare the next one if there is one. Else stop the
                    // resources.
                    lock.withLock {
                        if (book != null) {
                            Timber.v("onCompletion called, nextChapter=%s", book!!.nextChapter())
                            if (book!!.nextChapter() != null) {
                                next()
                            } else {
                                Timber.v("Reached last track. Stopping player")
                                stopUpdating()
                                playStateManager.playState.onNext(PlayStateManager.PlayState.STOPPED)

                                state = State.COMPLETED
                            }
                        }
                    }
                }

        player.errorObservable
                .subscribe {
                    // inform user on errors
                    lock.withLock {
                        Timber.e("onError")
                        if (book != null) {
                            c.startActivity(BookActivity.malformedFileIntent(c, book!!.currentFile))
                        } else {
                            val intent = Intent(c, BookShelfFragment::class.java)
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
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
            Timber.i("constructor called with ${book.name}")
            this.book = book
        }
    }

    /**
     * Prepares the current chapter set in book.
     */
    private fun prepare() {
        lock.withLock {
            if (book != null) {
                try {
                    player.prepare(book!!.currentChapter().file)
                    player.currentPosition = book!!.time
                    player.playbackSpeed = book!!.playbackSpeed
                    state = State.PAUSED
                } catch (e: IOException) {
                    Timber.e(e, "Error when preparing the player.")
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
        Timber.v("startUpdating")
        subscriptions.apply {
            if (!hasSubscriptions()) {
                // counts down the sleep sand
                val sleepUpdateInterval = 1000L
                add(Observable.interval(sleepUpdateInterval, TimeUnit.MILLISECONDS)
                        .filter { internalSleepSand.value > 0 } // only notify if there is still time left
                        .map { internalSleepSand.value - sleepUpdateInterval } // calculate the new time
                        .map { it.coerceAtLeast(0) } // but keep at least 0
                        .subscribe { internalSleepSand.onNext(it) })

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
        Timber.v("direction=%s", direction)
        lock.withLock {
            if (book != null) {
                val currentPos = player.currentPosition
                val duration = player.duration
                val delta = prefs.seekTime * 1000

                val seekTo = if ((direction == Direction.FORWARD)) currentPos + delta else currentPos - delta
                Timber.v("currentPos=%d, seekTo=%d, duration=%d", currentPos, seekTo, duration)

                if (seekTo < 0) {
                    previous(false)
                } else if (seekTo > duration) {
                    next()
                } else {
                    changePosition(seekTo, book!!.currentFile)
                }
            }
        }
    }

    /**
     * If current time is > 2000ms, seek to 0. Else play previous chapter if there is one.
     */
    fun previous(toNullOfNewTrack: Boolean) {
        lock.withLock {
            if (book != null) {
                val previousChapter = book!!.previousChapter()
                if (player.currentPosition > 2000 || previousChapter == null) {
                    player.currentPosition = 0
                    book = book!!.copy(time = 0)
                    db.updateBook(book!!)
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
            if (sleepTimerActive()) {
                // if its active use toggle to stop the sleep timer
                toggleSleepSand()
            }
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
     * Turns the sleep timer on or off.
     *
     * @return true if the timer is now active, false if it now inactive
     */
    fun toggleSleepSand() {
        Timber.i("toggleSleepSand. Left sleepTime is ${internalSleepSand.value}")
        lock.withLock {
            if (internalSleepSand.value > 0L) {
                Timber.i("sleepSand is active. cancelling now")
                internalSleepSand.onNext(-1L)
            } else {
                Timber.i("preparing new sleep sand")
                val minutes = prefs.sleepTime
                internalSleepSand.onNext(TimeUnit.MINUTES.toMillis(minutes.toLong()))
            }
        }
    }

    /**
     * Pauses the player. Also stops the updating mechanism which constantly updates the book to the
     * database.

     * @param rewind true if the player should automatically rewind a little bit.
     */
    fun pause(rewind: Boolean) {
        lock.withLock {
            Timber.v("pause acquired lock. state is=%s", state)
            if (book != null) {
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
                                book = book!!.copy(time = seekTo)
                            }
                        }
                        db.updateBook(book!!)

                        playStateManager.playState.onNext(PlayStateManager.PlayState.PAUSED)

                        state = State.PAUSED
                    }
                    else -> Timber.e("pause called in illegal state=%s", state)
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
            val nextChapter = book?.nextChapter()
            if (nextChapter != null) {
                changePosition(0, nextChapter.file)
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
            Timber.v("changePosition with time $time and file $file")
            if (book != null) {
                val changeFile = (book!!.currentChapter().file != file)
                Timber.v("changeFile=%s", changeFile)
                if (changeFile) {
                    val wasPlaying = (state == State.STARTED)
                    book = book!!.copy(currentFile = file, time = time)
                    db.updateBook(book!!)
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
                            book = book!!.copy(time = time)
                            db.updateBook(book!!)
                        }
                        else -> Timber.e("changePosition called in illegal state=%s", state)
                    }
                }
            }
        }
    }

    /**
     * The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc.
     */
    var playbackSpeed: Float
        get() {
            lock.withLock {
                return player.playbackSpeed
            }
        }
        set(speed) {
            lock.withLock {
                if (book != null) {
                    book = book!!.copy(playbackSpeed = speed)
                    db.updateBook(book!!)
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

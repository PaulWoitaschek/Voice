package de.ph1b.audiobook.mediaplayer

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import de.ph1b.audiobook.activity.BookActivity
import de.ph1b.audiobook.fragment.BookShelfFragment
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookShelf
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayState
import de.ph1b.audiobook.utils.Communication
import rx.subjects.BehaviorSubject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class MediaPlayerController
@Inject
constructor(private val c: Context, private val prefs: PrefsManager,
            private val communication: Communication, private val db: BookShelf,
            private val player: MediaPlayerInterface) {

    private val lock = ReentrantLock()
    private val executor = Executors.newScheduledThreadPool(2)
    public val playState = BehaviorSubject.create(PlayState.STOPPED)
    @Volatile var isSleepTimerActive = false
        private set
    private var sleepSand: ScheduledFuture<*>? = null
    var book: Book? = null
        private set
    @Volatile private var state: State = State.IDLE
    private var updater: ScheduledFuture<*>? = null
    @Volatile private var prepareTries = 0

    val leftSleepTimerTime: Long
        get() {
            if (sleepSand == null || sleepSand!!.isCancelled || sleepSand!!.isDone) {
                return 0
            } else {
                return sleepSand!!.getDelay(TimeUnit.MILLISECONDS)
            }
        }

    fun setPlayState(playState: PlayState) {
        this.playState.onNext(playState)
    }

    /**
     * Initializes a new book. After this, a call to play can be made.

     * @param book The book to be initialized.
     */
    fun init(book: Book) {
        lock.withLock {
            Timber.i("constructor called with book=%s", book)
            this.book = book
        }
    }

    fun updateBook(book: Book?) {
        lock.withLock {
            this.book = book
        }
    }

    /**
     * Prepares the current chapter set in book.
     */
    private fun prepare() {
        lock.withLock {
            if (book != null) {
                player.reset()

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
                                        setPlayState(PlayState.STOPPED)

                                        state = State.PLAYBACK_COMPLETED
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

                                state = State.DEAD
                            }
                        }

                player.setWakeMode(c, PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE)

                try {
                    player.setDataSource(book!!.currentChapter().file.absolutePath)
                    player.prepare()
                    player.currentPosition = book!!.time
                    player.playbackSpeed = book!!.playbackSpeed
                    state = State.PREPARED
                } catch (e: IOException) {
                    Timber.e(e, "Error when preparing the player.")
                    state = State.DEAD
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
                MediaPlayerController.State.PLAYBACK_COMPLETED -> {
                    player.currentPosition = 0
                    player.start()
                    startUpdating()
                    setPlayState(PlayState.PLAYING)
                    state = State.STARTED
                    prepareTries = 0
                }
                MediaPlayerController.State.PREPARED, MediaPlayerController.State.PAUSED -> {
                    player.start()
                    startUpdating()
                    setPlayState(PlayState.PLAYING)
                    state = State.STARTED
                    prepareTries = 0
                }
                MediaPlayerController.State.DEAD, MediaPlayerController.State.IDLE -> {
                    if (prepareTries > 5) {
                        prepareTries = 0
                        state = State.DEAD
                    } else {
                        prepare()
                        prepareTries++
                        play()
                    }
                }
                else -> Timber.e("play called in illegal state=%s", state)
            }
        }
    }

    /**
     * Updates the current time and position of the book, writes it to the database and sends
     * updates to the GUI.
     */
    private fun startUpdating() {
        Timber.v("startUpdating")
        if (!updaterActive()) {
            updater = executor.scheduleAtFixedRate({
                lock.withLock {
                    book = book?.copy(time = player.currentPosition)
                    if (book != null) {
                        db.updateBook(book!!)
                    }
                }
            }, 0, 1, TimeUnit.SECONDS)
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
            stopUpdating()
            player.reset()
            setPlayState(PlayState.STOPPED)
            if (sleepSandActive()) {
                toggleSleepSand()
            }
            state = State.IDLE
        }
    }

    /**
     * Stops updating the book with the current position.
     */
    private fun stopUpdating() {
        if (updaterActive()) {
            updater!!.cancel(true)
        }
    }

    /**
     * @return true if a sleep timer has been set.
     */
    private fun sleepSandActive(): Boolean {
        lock.withLock {
            return sleepSand != null && !sleepSand!!.isCancelled && !sleepSand!!.isDone
        }
    }

    /**
     * Turns the sleep timer on or off.
     */
    fun toggleSleepSand() {
        Timber.i("toggleSleepSand. Old state was:%b", sleepSandActive())
        lock.withLock {
            if (sleepSandActive()) {
                Timber.i("sleepSand is active. cancelling now")
                sleepSand!!.cancel(false)
                isSleepTimerActive = false
            } else {
                Timber.i("preparing new sleep sand")
                val minutes = prefs.sleepTime
                isSleepTimerActive = true
                sleepSand = executor.schedule({
                    lock.withLock {
                        pause(true)
                        isSleepTimerActive = false
                        communication.sleepStateChanged()
                    }
                }, minutes.toLong(), TimeUnit.MINUTES)
            }
            communication.sleepStateChanged()
        }
    }

    /**
     * @return true if the position updater is active.
     */
    private fun updaterActive(): Boolean {
        return updater != null && !updater!!.isCancelled && !updater!!.isDone
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
                    MediaPlayerController.State.STARTED -> {
                        player.pause()
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

                        setPlayState(PlayState.PAUSED)

                        state = State.PAUSED
                    }
                    else -> Timber.e("pause called in illegal state=%s", state)
                }
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
            Timber.v("time=%d, relPath=%s", time, file)
            if (book != null) {
                val changeFile = (book!!.currentChapter().file != file)
                Timber.v("changeFile=%s", changeFile)
                if (changeFile) {
                    val wasPlaying = (state == State.STARTED)
                    book = book!!.copy(currentFile = file, time = time)
                    db.updateBook(book!!)
                    prepare()
                    if (wasPlaying) {
                        player.start()
                        state = State.STARTED
                        setPlayState(PlayState.PLAYING)
                    } else {
                        state = State.PREPARED
                        setPlayState(PlayState.PAUSED)
                    }
                } else {
                    when (state) {
                        MediaPlayerController.State.PREPARED, MediaPlayerController.State.STARTED, MediaPlayerController.State.PAUSED, MediaPlayerController.State.PLAYBACK_COMPLETED -> {
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
                    if (state != State.DEAD) {
                        player.playbackSpeed = speed
                    } else {
                        Timber.e("setPlaybackSpeed called in illegal state=%s", state)
                    }
                }
            }
        }

    /**
     * After this this object should no longer be used.
     */
    fun onDestroy() {
        player.release()
        if (sleepSand != null && !sleepSand!!.isCancelled && !sleepSand!!.isDone) {
            sleepSand!!.cancel(false)
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
        DEAD,
        PREPARED,
        STARTED,
        PLAYBACK_COMPLETED,
        IDLE
    }
}

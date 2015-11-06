package de.ph1b.audiobook.mediaplayer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ph1b.audiobook.activity.BookActivity;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.persistence.BookShelf;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.utils.Communication;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

@Singleton
public class MediaPlayerController implements MediaPlayer.OnErrorListener,
        MediaPlayerInterface.OnCompletionListener {

    private final Context c;
    private final ReentrantLock lock = new ReentrantLock();
    private final PrefsManager prefs;
    private final BookShelf db;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final MediaPlayerInterface player;
    private final Communication communication;
    private final BehaviorSubject<PlayState> playState = BehaviorSubject.create(PlayState.STOPPED);
    private volatile boolean sleepTimerActive = false;
    @Nullable
    private ScheduledFuture<?> sleepSand;
    @Nullable
    private Book book;
    private volatile State state;
    private ScheduledFuture updater = null;
    private volatile int prepareTries = 0;

    @Inject
    public MediaPlayerController(@NonNull final Context c, PrefsManager prefs,
                                 Communication communication, BookShelf bookShelf,
                                 MediaPlayerInterface player) {
        this.c = c;
        this.prefs = prefs;
        this.communication = communication;
        this.db = bookShelf;
        this.player = player;

        state = State.IDLE;
    }

    public boolean isSleepTimerActive() {
        return sleepTimerActive;
    }

    public long getLeftSleepTimerTime() {
        if (sleepSand == null || sleepSand.isCancelled() || sleepSand.isDone()) {
            return 0;
        } else {
            return sleepSand.getDelay(TimeUnit.MILLISECONDS);
        }
    }


    public BehaviorSubject<PlayState> getPlayState() {
        return playState;
    }

    public void setPlayState(PlayState playState) {
        this.playState.onNext(playState);
    }

    /**
     * Initializes a new book. After this, a call to play can be made.
     *
     * @param book The book to be initialized.
     */
    public void init(@NonNull Book book) {
        lock.lock();
        try {
            Timber.i("constructor called with book=%s", book);
            Preconditions.checkNotNull(book);
            this.book = book;
            prepare();
        } finally {
            lock.unlock();
        }
    }

    public void updateBook(@Nullable Book book) {
        lock.lock();
        try {
            this.book = book;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Prepares the current chapter set in book.
     */
    private void prepare() {
        lock.lock();
        try {
            if (book != null) {
                player.reset();

                player.setOnCompletionListener(this);
                player.setOnErrorListener(this);
                player.setWakeMode(c, PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE);

                try {
                    player.setDataSource(book.currentChapter().file().getAbsolutePath());
                    player.prepare();
                    player.seekTo(book.time());
                    player.setPlaybackSpeed(book.playbackSpeed());
                    state = State.PREPARED;
                } catch (IOException e) {
                    e.printStackTrace();
                    state = State.DEAD;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Plays the prepared file.
     */
    public void play() {
        lock.lock();
        try {
            switch (state) {
                case PLAYBACK_COMPLETED:
                    player.seekTo(0);
                case PREPARED:
                case PAUSED:
                    player.start();
                    startUpdating();
                    setPlayState(PlayState.PLAYING);
                    state = State.STARTED;
                    prepareTries = 0;
                    break;
                case DEAD:
                case IDLE:
                    if (prepareTries > 5) {
                        prepareTries = 0;
                        state = State.DEAD;
                        break;
                    }
                    prepare();
                    prepareTries++;
                    play();
                    break;
                default:
                    Timber.e("play called in illegal state=%s", state);
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates the current time and position of the book, writes it to the database and sends
     * updates to the GUI.
     */
    private void startUpdating() {
        Timber.v("startUpdating");
        if (!updaterActive()) {
            updater = executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    try {
                        if (book != null) {
                            book = Book.builder(book)
                                    .time(player.getCurrentPosition())
                                    .build();
                            db.updateBook(book);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Skips by the amount, specified in the settings.
     *
     * @param direction The direction to skip
     */
    public void skip(Direction direction) {
        Timber.v("direction=%s", direction);
        lock.lock();
        try {
            if (book != null) {
                final int currentPos = player.getCurrentPosition();
                final int duration = player.getDuration();
                final int delta = prefs.getSeekTime() * 1000;

                final int seekTo = (direction == Direction.FORWARD) ? currentPos + delta : currentPos - delta;
                Timber.v("currentPos=%d, seekTo=%d, duration=%d", currentPos, seekTo, duration);

                if (seekTo < 0) {
                    previous(false);
                } else if (seekTo > duration) {
                    next();
                } else {
                    changePosition(seekTo, book.currentFile());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * If current time is > 2000ms, seek to 0. Else play previous chapter if there is one.
     */
    public void previous(boolean toNullOfNewTrack) {
        lock.lock();
        try {
            if (book != null) {
                Chapter previousChapter = book.previousChapter();
                if (player.getCurrentPosition() > 2000 || previousChapter == null) {
                    player.seekTo(0);
                    book = Book.builder(book)
                            .time(0)
                            .build();
                    db.updateBook(book);
                } else {
                    if (toNullOfNewTrack) {
                        changePosition(0, previousChapter.file());
                    } else {
                        changePosition(previousChapter.duration() -
                                (prefs.getSeekTime() * 1000), previousChapter.file());
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the current book.
     */
    @Nullable
    public Book getBook() {
        return book;
    }

    /**
     * Stops the playback and releases some resources.
     */
    public void stop() {
        lock.lock();
        try {
            stopUpdating();
            player.reset();
            setPlayState(PlayState.STOPPED);
            if (sleepSandActive()) {
                toggleSleepSand();
            }
            state = State.IDLE;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops updating the book with the current position.
     */
    private void stopUpdating() {
        if (updaterActive()) {
            updater.cancel(true);
        }
    }

    /**
     * @return true if a sleep timer has been set.
     */
    private boolean sleepSandActive() {
        lock.lock();
        try {
            return sleepSand != null && !sleepSand.isCancelled() && !sleepSand.isDone();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Turns the sleep timer on or off.
     */
    public void toggleSleepSand() {
        Timber.i("toggleSleepSand. Old state was:%b", sleepSandActive());
        lock.lock();
        try {
            if (sleepSandActive()) {
                assert sleepSand != null;
                Timber.i("sleepSand is active. cancelling now");
                sleepSand.cancel(false);
                sleepTimerActive = false;
            } else {
                Timber.i("preparing new sleep sand");
                final int minutes = prefs.getSleepTime();
                sleepTimerActive = true;
                sleepSand = executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        lock.lock();
                        try {
                            pause(true);
                            sleepTimerActive = false;
                            communication.sleepStateChanged();
                        } finally {
                            lock.unlock();
                        }
                    }
                }, minutes, TimeUnit.MINUTES);
            }
            communication.sleepStateChanged();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return true if the position updater is active.
     */
    private boolean updaterActive() {
        return updater != null && !updater.isCancelled() && !updater.isDone();
    }

    /**
     * Pauses the player. Also stops the updating mechanism which constantly updates the book to the
     * database.
     *
     * @param rewind true if the player should automatically rewind a little bit.
     */
    public void pause(boolean rewind) {
        lock.lock();
        try {
            Timber.v("pause acquired lock. state is=%s", state);
            if (book != null) {
                switch (state) {
                    case STARTED:
                        player.pause();
                        stopUpdating();

                        if (rewind) {
                            final int autoRewind = prefs.getAutoRewindAmount() * 1000;
                            if (autoRewind != 0) {
                                int originalPosition = player.getCurrentPosition();
                                int seekTo = originalPosition - autoRewind;
                                seekTo = Math.max(seekTo, 0);
                                player.seekTo(seekTo);
                                book = Book.builder(book)
                                        .time(seekTo)
                                        .build();
                            }
                        }
                        db.updateBook(book);

                        setPlayState(PlayState.PAUSED);

                        state = State.PAUSED;
                        break;
                    default:
                        Timber.e("pause called in illegal state=%s", state);
                        break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        lock.lock();
        try {
            Timber.e("onError");
            if (book != null) {
                c.startActivity(BookActivity.malformedFileIntent(c, book.currentFile()));
            } else {
                Intent intent = new Intent(c, BookShelfFragment.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(intent);
            }

            state = State.DEAD;
        } finally {
            lock.unlock();
        }

        return false;
    }

    /**
     * After the current song has ended, prepare the next one if there is one. Else stop the
     * resources.
     */
    @Override
    public void onCompletion() {
        lock.lock();
        try {
            if (book != null) {
                Timber.v("onCompletion called, nextChapter=%s", book.nextChapter());
                if (book.nextChapter() != null) {
                    next();
                } else {
                    Timber.v("Reached last track. Stopping player");
                    stopUpdating();
                    setPlayState(PlayState.STOPPED);

                    state = State.PLAYBACK_COMPLETED;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Plays the next chapter. If there is none, don't do anything.
     */
    public void next() {
        lock.lock();
        try {
            if (book != null) {
                Chapter nextChapter = book.nextChapter();
                if (nextChapter != null) {
                    changePosition(0, nextChapter.file());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Changes the current position in book. If the path is the same, continues playing the song.
     * Else calls {@link #prepare()} to prepare the next file
     *
     * @param time The time in chapter at which to start
     * @param file The path of the media to play (relative to the books root path)
     */
    public void changePosition(int time, File file) {
        lock.lock();
        try {
            Timber.v("time=%d, relPath=%s", time, file);
            if (book != null) {
                boolean changeFile = (!book.currentChapter().file().equals(file));
                Timber.v("changeFile=%s", changeFile);
                if (changeFile) {
                    boolean wasPlaying = (state == State.STARTED);
                    book = Book.builder(book)
                            .time(time)
                            .currentFile(file)
                            .build();
                    db.updateBook(book);
                    prepare();
                    if (wasPlaying) {
                        player.start();
                        state = State.STARTED;
                        setPlayState(PlayState.PLAYING);
                    } else {
                        state = State.PREPARED;
                        setPlayState(PlayState.PAUSED);
                    }
                } else {
                    switch (state) {
                        case PREPARED:
                        case STARTED:
                        case PAUSED:
                        case PLAYBACK_COMPLETED:
                            player.seekTo(time);
                            book = Book.builder(book)
                                    .time(time)
                                    .build();
                            db.updateBook(book);
                            break;
                        default:
                            Timber.e("changePosition called in illegal state=%s", state);
                            break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public float getPlaybackSpeed() {
        lock.lock();
        try {
            return player.getPlaybackSpeed();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets the current playback speed
     *
     * @param speed The playback-speed. 1.0 for normal playback, 2.0 for twice the speed, etc.
     */
    public void setPlaybackSpeed(float speed) {
        lock.lock();
        try {
            if (book != null) {
                book = Book.builder(book)
                        .playbackSpeed(speed)
                        .build();
                db.updateBook(book);
                if (state != State.DEAD) {
                    player.setPlaybackSpeed(speed);
                } else {
                    Timber.e("setPlaybackSpeed called in illegal state=%s", state);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * After this this object should no longer be used.
     */
    public void onDestroy() {
        player.release();
        if (sleepSand != null && !sleepSand.isCancelled() && !sleepSand.isDone()) {
            sleepSand.cancel(false);
        }
    }

    public enum PlayState {
        PLAYING,
        PAUSED,
        STOPPED,
    }

    /**
     * The direction to skip.
     */
    public enum Direction {
        FORWARD, BACKWARD
    }

    /**
     * The various internal states the player can have.
     */
    private enum State {
        PAUSED,
        DEAD,
        PREPARED,
        STARTED,
        PLAYBACK_COMPLETED,
        IDLE
    }
}

package de.ph1b.audiobook.mediaplayer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import net.jcip.annotations.GuardedBy;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.activity.BookActivity;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;
import de.ph1b.audiobook.utils.Validate;

public class MediaPlayerController implements MediaPlayer.OnErrorListener, MediaPlayerInterface.OnCompletionListener {


    public static final String MALFORMED_FILE = "malformedFile";
    public static final boolean playerCanSetSpeed = Build.VERSION.SDK_INT >= 16;
    private static final String TAG = MediaPlayerController.class.getSimpleName();
    private final Context c;
    private final ReentrantLock lock = new ReentrantLock();
    private final PrefsManager prefs;
    private final DataBaseHelper db;
    private final BaseApplication baseApplication;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    @GuardedBy("lock")
    private final MediaPlayerInterface player;
    private Book book;
    private volatile State state;
    private ScheduledFuture<?> sleepSand;
    private volatile boolean stopAfterCurrentTrack = false;
    private ScheduledFuture updater = null;
    private volatile int prepareTries = 0;

    public MediaPlayerController(@NonNull final BaseApplication baseApplication) {
        lock.lock();
        try {
            this.c = baseApplication.getApplicationContext();
            prefs = new PrefsManager(c);
            db = DataBaseHelper.getInstance(c);
            this.baseApplication = baseApplication;

            if (playerCanSetSpeed) {
                player = new CustomMediaPlayer();
            } else {
                player = new AndroidMediaPlayer();
            }
        } finally {
            lock.unlock();
        }
    }

    public void init(@NonNull Book book) {
        lock.lock();
        try {
            L.e(TAG, "constructor called with book=" + book);
            new Validate().notNull(book);
            this.book = book;
            prepare();
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
            player.reset();

            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setWakeMode(c, PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE);

            try {
                player.setDataSource(book.getRoot() + "/" + book.getCurrentChapter().getPath());
                player.prepare();
                player.seekTo(book.getTime());
                player.setPlaybackSpeed(book.getPlaybackSpeed());
                state = State.PREPARED;
            } catch (IOException e) {
                e.printStackTrace();
                state = State.DEAD;
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
                    baseApplication.setPlayState(BaseApplication.PlayState.PLAYING);
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
                    L.e(TAG, "play called in illegal state:" + state);
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
        L.v(TAG, "startupdating");
        if (!updaterActive()) {
            updater = executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    try {
                        book.setPosition(player.getCurrentPosition(), book.getRelativeMediaPath());
                        db.updateBook(book);
                        baseApplication.notifyPositionChanged(false);
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
        lock.lock();
        try {
            int currentPos = player.getCurrentPosition();
            int duration = player.getDuration();
            int delta = prefs.getSeekTime() * 1000;

            int seekTo = (direction == Direction.FORWARD) ? currentPos + delta : currentPos - delta;

            if (seekTo < 0) {
                previous(false);
            } else if (seekTo > duration) {
                next();
            } else {
                changePosition(seekTo, book.getRelativeMediaPath());
            }
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
            book.setPlaybackSpeed(speed);
            db.updateBook(book);
            if (state != State.DEAD) {
                player.setPlaybackSpeed(speed);
            } else {
                L.e(TAG, "setPlaybackSpeed called in illegal state: " + state);
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
            if (player.getCurrentPosition() > 2000 || book.getPreviousChapter() == null) {
                player.seekTo(0);
                book.setPosition(0, book.getRelativeMediaPath());
                db.updateBook(book);
                baseApplication.notifyPositionChanged(false);
            } else {
                if (toNullOfNewTrack) {
                    changePosition(0, book.getPreviousChapter().getPath());
                } else {
                    changePosition(book.getPreviousChapter().getDuration() - (prefs.getSeekTime() * 1000), book.getPreviousChapter().getPath());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public Book getBook() {
        return book;
    }

    /**
     * Releases the controller. After this, this object should no longer be used.
     */
    public void stop() {
        lock.lock();
        try {
            stopUpdating();
            player.reset();
            baseApplication.setPlayState(BaseApplication.PlayState.STOPPED);
            if (sleepSandActive()) {
                toggleSleepSand();
            }
            state = State.IDLE;
        } finally {
            lock.unlock();
        }
    }

    private void stopUpdating() {
        if (updaterActive()) {
            updater.cancel(true);
        }
    }

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
        L.i(TAG, "toggleSleepSand. Old state was:" + sleepSandActive());
        lock.lock();
        try {
            if (sleepSandActive()) {
                L.i(TAG, "sleepSand is active. cancelling now");
                sleepSand.cancel(false);
                stopAfterCurrentTrack = true;
                baseApplication.setSleepTimerActive(false);
            } else {
                L.i(TAG, "preparing new sleep sand");
                int minutes = prefs.getSleepTime();
                stopAfterCurrentTrack = prefs.stopAfterCurrentTrack();
                baseApplication.setSleepTimerActive(true);
                sleepSand = executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (!stopAfterCurrentTrack) {
                            lock.lock();
                            try {
                                pause();
                                baseApplication.setSleepTimerActive(false);
                            } finally {
                                lock.unlock();
                            }
                        } else {
                            L.d(TAG, "Sandman: We are not stopping right now. We stop after this track.");
                        }
                    }
                }, minutes, TimeUnit.MINUTES);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean updaterActive() {
        return updater != null && !updater.isCancelled() && !updater.isDone();
    }

    /**
     * Pauses the player. Also stops the updating mechanism which constantly updates the book to the
     * database.
     */
    public void pause() {
        lock.lock();
        try {
            L.v(TAG, "pause acquired lock. state is=" + state);
            switch (state) {
                case STARTED:
                    player.pause();
                    stopUpdating();
                    baseApplication.setPlayState(BaseApplication.PlayState.PAUSED);
                    state = State.PAUSED;
                    break;
                default:
                    L.e(TAG, "pause called in illegal state=" + state);
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        lock.lock();
        try {
            L.e(TAG, "onError");
            baseApplication.deleteBook(book);
            Intent bookShelfIntent = BookActivity.bookScreenIntent(c);
            bookShelfIntent.putExtra(MALFORMED_FILE, book.getRoot() + "/" + book.getCurrentChapter().getPath());
            c.startActivity(bookShelfIntent);

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
            L.v(TAG, "onCompletion called, nextChapter=" + book.getNextChapter());
            if (book.getNextChapter() != null) {
                next();
            } else {
                L.v(TAG, "Reached last track. Stopping player");
                stopUpdating();
                baseApplication.setPlayState(BaseApplication.PlayState.STOPPED);

                state = State.PLAYBACK_COMPLETED;
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
            Chapter nextChapter = book.getNextChapter();
            if (nextChapter != null) {
                changePosition(0, nextChapter.getPath());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Changes the current position in book. If the path is the same, continues playing the song.
     * Else calls {@link #prepare()} to prepare the next file
     *
     * @param time    The time in chapter at which to start
     * @param relPath The relative path of the media to play (relative to the books root path)
     */
    public void changePosition(int time, String relPath) {
        lock.lock();
        try {
            boolean changeFile = (!book.getCurrentChapter().getPath().equals(relPath));
            if (changeFile) {
                boolean wasPlaying = (state == State.STARTED);
                book.setPosition(time, relPath);
                db.updateBook(book);
                prepare();
                if (wasPlaying) {
                    player.start();
                    state = State.STARTED;
                    baseApplication.setPlayState(BaseApplication.PlayState.PLAYING);
                } else {
                    state = State.PREPARED;
                    baseApplication.setPlayState(BaseApplication.PlayState.PAUSED);
                }
                baseApplication.notifyPositionChanged(true);
            } else {
                switch (state) {
                    case PREPARED:
                    case STARTED:
                    case PAUSED:
                    case PLAYBACK_COMPLETED:
                        player.seekTo(time);
                        book.setPosition(time, book.getCurrentChapter().getPath());
                        db.updateBook(book);
                        baseApplication.notifyPositionChanged(false);
                        break;
                    default:
                        L.e(TAG, "changePosition called in illegal state:" + state);
                        break;
                }
            }
        } finally {
            lock.unlock();
        }
    }


    public enum Direction {
        FORWARD, BACKWARD
    }

    private enum State {
        PAUSED,
        DEAD,
        PREPARED,
        STARTED,
        PLAYBACK_COMPLETED,
        IDLE
    }
}

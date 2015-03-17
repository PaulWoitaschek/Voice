package de.ph1b.audiobook.mediaplayer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.activity.BookShelfActivity;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.utils.ArgumentValidator;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

public class MediaPlayerController implements MediaPlayer.OnErrorListener, MediaPlayerInterface.OnCompletionListener {

    private static final String TAG = MediaPlayerController.class.getSimpleName();
    public static boolean playerCanSetSpeed = Build.VERSION.SDK_INT >= 16;
    private final Context c;
    private final ReentrantLock lock = new ReentrantLock();
    private final PrefsManager prefs;
    private final DataBaseHelper db;
    private final BaseApplication baseApplication;
    private final Book book;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final MediaPlayerInterface player;
    private volatile State state;
    private ScheduledFuture<?> sleepSand;
    private volatile boolean stopAfterCurrentTrack = false;
    private ScheduledFuture updater = null;

    public MediaPlayerController(BaseApplication baseApplication, Book book) {
        L.e(TAG, "constructor called with book=" + book);
        ArgumentValidator.validate(baseApplication, book);
        this.c = baseApplication.getApplicationContext();
        this.book = book;
        prefs = new PrefsManager(c);
        db = DataBaseHelper.getInstance(c);
        this.baseApplication = baseApplication;

        if (playerCanSetSpeed) {
            player = new CustomMediaPlayer();
        } else {
            player = new AndroidMediaPlayer();
        }

        prepare();
    }


    /**
     * Prepares the current chapter set in book.
     */
    private void prepare() {
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
    }


    /**
     * Pauses the player. Also stops the updating mechanism which constantly updates the book to the
     * database.
     */
    public void pause() {
        lock.lock();
        try {
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
                    break;
                case DEAD:
                case IDLE:
                    prepare();
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
        if (!updaterActive()) {
            updater = executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    try {
                        book.setPosition(player.getCurrentPosition(), book.getRelativeMediaPath());
                        db.updateBook(book);
                        baseApplication.notifyPositionChanged();
                    } finally {
                        lock.unlock();
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }


    private boolean updaterActive() {
        return updater != null && !updater.isCancelled() && !updater.isDone();
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
     * Changes the current position in book. If the path is the same, continues playing the song.
     * Else calls {@link #prepare()} to prepare the next file
     *
     * @param time    The time in chapter at which to start
     * @param relPath The relative path of the media to play (relative to the books root path)
     */
    public void changePosition(int time, String relPath) {
        L.v(TAG, "changePosition(" + time + "/" + relPath + ")");
        lock.lock();
        try {
            boolean changeFile = (!book.getCurrentChapter().getPath().equals(relPath));
            if (changeFile) {
                boolean wasPlaying = (state == State.STARTED);
                book.setPosition(time, relPath);
                db.updateBook(book);
                baseApplication.notifyPositionChanged();
                prepare();
                if (wasPlaying) {
                    player.start();
                    state = State.STARTED;
                    baseApplication.setPlayState(BaseApplication.PlayState.PLAYING);
                } else {
                    state = State.PREPARED;
                    baseApplication.setPlayState(BaseApplication.PlayState.PAUSED);
                }
                baseApplication.notifyPositionChanged();
            } else {
                switch (state) {
                    case PREPARED:
                    case STARTED:
                    case PAUSED:
                    case PLAYBACK_COMPLETED:
                        player.seekTo(time);
                        book.setPosition(time, book.getCurrentChapter().getPath());
                        db.updateBook(book);
                        baseApplication.notifyPositionChanged();
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

    private boolean sleepSandActive() {
        return sleepSand != null && !sleepSand.isCancelled() && !sleepSand.isDone();
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
                L.i(TAG, "preparing new sleepsand");
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
     * If current time is > 2000ms, seek to 0. Else play previous chapter if there is one.
     */
    public void previous(boolean toNullOfNewTrack) {
        lock.lock();
        try {
            if (player.getCurrentPosition() > 2000 || book.getPreviousChapter() == null) {
                player.seekTo(0);
                book.setPosition(0, book.getRelativeMediaPath());
                db.updateBook(book);
                baseApplication.notifyPositionChanged();
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
    public void release() {
        L.i(TAG, "release called");
        lock.lock();
        try {
            stopUpdating();
            player.release();
            baseApplication.setPlayState(BaseApplication.PlayState.STOPPED);
            baseApplication.setSleepTimerActive(false);
            executor.shutdown();
            state = State.DEAD;
        } finally {
            lock.unlock();
        }
    }

    private void stopUpdating() {
        if (updaterActive()) {
            updater.cancel(true);
        }
    }


    /**
     * After the current song has ended, prepare the next one if there is one. Else release the
     * resources.
     */


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        File currentFile = new File(book.getCurrentChapter().getPath());
        if (!currentFile.exists()) {
            db.deleteBook(book);
            baseApplication.getAllBooks().remove(book);
            Intent bookShelfIntent = new Intent(c, BookShelfActivity.class);
            bookShelfIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            c.startActivity(bookShelfIntent);
        }

        return false;
    }

    @Override
    public void onCompletion() {
        L.v(TAG, "onCompletion called, nextChapter=" + book.getNextChapter());
        if (book.getNextChapter() != null) {
            next();
        } else {
            L.v(TAG, "Reached last track. Stopping player");
            stopUpdating();
            baseApplication.setPlayState(BaseApplication.PlayState.STOPPED);

            state = State.PLAYBACK_COMPLETED;
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

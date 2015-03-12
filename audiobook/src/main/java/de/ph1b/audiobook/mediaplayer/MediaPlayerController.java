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

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.activity.BookShelfActivity;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.BaseApplication.PlayState;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

public class MediaPlayerController implements MediaPlayer.OnErrorListener, MediaPlayerInterface.OnCompletionListener {

    private static final String TAG = MediaPlayerController.class.getSimpleName();
    private final Context c;
    private final ReentrantLock lock = new ReentrantLock();
    private final PrefsManager prefs;
    private final MediaPlayerInterface mediaPlayer;
    private final DataBaseHelper db;
    private final BaseApplication baseApplication;
    private final Book book;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private volatile State state;
    private ScheduledFuture<?> sleepSand;
    private volatile boolean stopAfterCurrentTrack = false;
    private ScheduledFuture updater = null;

    public MediaPlayerController(BaseApplication baseApplication, Book book) {
        this.c = baseApplication.getApplicationContext();
        this.book = book;
        prefs = new PrefsManager(c);
        db = DataBaseHelper.getInstance(c);
        this.baseApplication = baseApplication;

        boolean useUnstableMediaPlayer = false;
        //noinspection ConstantConditions
        if (useUnstableMediaPlayer && BuildConfig.DEBUG) {
            mediaPlayer = new ExoUnstableMediaPlayer(c);
        } else if (Build.VERSION.SDK_INT >= 16) {
            mediaPlayer = new CustomMediaPlayer();
        } else {
            mediaPlayer = new AndroidMediaPlayer();
        }
        mediaPlayer.setOnErrorListener(this);

        prepare();
    }

    public Book getBook() {
        return book;
    }

    private void prepare() {
        if (state != State.IDLE) {
            mediaPlayer.reset();
            state = State.IDLE;
        }

        mediaPlayer.setWakeMode(c, PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE);
        mediaPlayer.setOnCompletionListener(this);

        try {
            mediaPlayer.setDataSource(book.getRoot() + "/" + book.getCurrentChapter().getPath());
            mediaPlayer.prepare();
            int seekTo = book.getTime();
            mediaPlayer.seekTo(seekTo);
            setPlaybackSpeed(book.getPlaybackSpeed());
            state = State.PREPARED;
        } catch (IOException e) {
            e.printStackTrace();
            state = State.DEAD;
        }

    }

    public void release() {
        L.i(TAG, "release called");
        lock.lock();
        try {
            mediaPlayer.release();
            stopUpdating();

            baseApplication.setPlayState(PlayState.STOPPED);
            baseApplication.setSleepTimerActive(false);
            executor.shutdown();
            state = State.DEAD;
        } finally {
            lock.unlock();
        }
    }

    public void play() {
        lock.lock();
        try {
            switch (state) {
                case PREPARED:
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    mediaPlayer.start();

                    startUpdating();

                    baseApplication.setPlayState(PlayState.PLAYING);
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

    private boolean updaterActive() {
        return updater != null && !updater.isCancelled() && !updater.isDone();
    }

    private void startUpdating() {
        if (!updaterActive()) {
            updater = executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    try {
                        book.setPosition(mediaPlayer.getCurrentPosition(), book.getRelativeMediaPath());
                        db.updateBook(book);
                        baseApplication.notifyPositionChanged();
                    } finally {
                        lock.unlock();
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    private void stopUpdating() {
        if (updaterActive()) {
            updater.cancel(true);
        }
    }

    private boolean sleepSandActive() {
        return sleepSand != null && !sleepSand.isCancelled() && !sleepSand.isDone();
    }

    public void toggleSleepSand() {
        L.i(TAG, "toggleSleepSand. Old state was:" + sleepSandActive());
        lock.lock();
        try {
            if (sleepSandActive()) {
                L.i(TAG, "sleepsand is active. cancelling now");
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
                                release();
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

    public void changeTime(int time, String relPath) {
        L.v(TAG, "changeTime(" + time + "/" + relPath + ")");
        lock.lock();
        try {
            boolean changeFile = (!book.getCurrentChapter().getPath().equals(relPath));
            if (changeFile) {
                boolean wasPlaying = (state == State.STARTED);

                mediaPlayer.reset();
                state = State.IDLE;

                book.setPosition(time, relPath);
                db.updateBook(book);
                baseApplication.notifyPositionChanged();

                prepare();

                if (wasPlaying) {
                    mediaPlayer.start();
                    state = State.STARTED;
                    baseApplication.setPlayState(PlayState.PLAYING);
                } else {
                    state = State.PREPARED;
                    baseApplication.setPlayState(PlayState.PAUSED);
                }
                baseApplication.notifyPositionChanged();
            } else {
                switch (state) {
                    case PREPARED:
                    case STARTED:
                    case PAUSED:
                    case PLAYBACK_COMPLETED:
                        mediaPlayer.seekTo(time);
                        book.setPosition(time, book.getCurrentChapter().getPath());
                        db.updateBook(book);
                        baseApplication.notifyPositionChanged();
                        break;
                    default:
                        L.e(TAG, "changeTime called in illegal state:" + state);
                        break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void pause() {
        lock.lock();
        try {
            switch (state) {
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    mediaPlayer.pause();
                    stopUpdating();

                    baseApplication.setPlayState(PlayState.PAUSED);
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

    public void setPlaybackSpeed(float speed) {
        lock.lock();
        try {
            book.setPlaybackSpeed(speed);
            db.updateBook(book);
            if (state != State.DEAD) {
                mediaPlayer.setPlaybackSpeed(speed);
            } else {
                L.e(TAG, "setPlaybackSpeed called in illegal state: " + state);
            }
        } finally {
            lock.unlock();
        }
    }

    public void skip(Direction direction) {
        lock.lock();
        try {
            int delta = prefs.getSeekTime() * 1000;
            if (direction == Direction.BACKWARD) {
                changeTime(book.getTime() - delta, book.getRelativeMediaPath());
            } else {
                changeTime(book.getTime() + delta, book.getRelativeMediaPath());
            }
        } finally {
            lock.unlock();
        }
    }

    public void next() {
        lock.lock();
        try {
            String nextChapter = book.getNextChapter().getPath();
            if (nextChapter != null) {
                changeTime(0, nextChapter);
            }
        } finally {
            lock.unlock();
        }
    }

    public void previous() {
        lock.lock();
        try {
            if (mediaPlayer.getCurrentPosition() > 2000 || book.getChapters().indexOf(book.getCurrentChapter()) == 0) {
                mediaPlayer.seekTo(0);
                book.setPosition(0, book.getRelativeMediaPath());
                db.updateBook(book);
                baseApplication.notifyPositionChanged();
            } else {
                changeTime(0, book.getPreviousChapter().getPath());
            }
        } finally {
            lock.unlock();
        }
    }

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
    public void onCompletion(MediaPlayerInterface mp) {
        if (book.getNextChapter() != null) {
            next();
        } else {
            L.v(TAG, "Reached last track. Stopping player");
            stopUpdating();
            baseApplication.setPlayState(PlayState.STOPPED);
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

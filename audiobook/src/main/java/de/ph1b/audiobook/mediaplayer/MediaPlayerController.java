package de.ph1b.audiobook.mediaplayer;


import android.content.Context;
import android.os.PowerManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.PositionUpdater;
import de.ph1b.audiobook.service.StateManager;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.Prefs;

public class MediaPlayerController {

    private static final String TAG = "MediaPlayerController";
    private final Book book;
    private final Context c;
    private final StateManager extState;
    private final ReentrantLock lock = new ReentrantLock();
    private final Prefs prefs;
    private final PositionUpdater positionUpdater;
    private final MediaPlayerCompat mediaPlayer;
    private final DataBaseHelper db;
    private final ScheduledExecutorService sandMan = Executors.newSingleThreadScheduledExecutor();
    private final MediaPlayerCompat.OnCompletionListener onCompletionListener = new MediaPlayerCompat.OnCompletionListener() {
        @Override
        public void onCompletion() {
            if (book.getPosition() + 1 < book.getContainingMedia().size()) {
                next();
            } else {
                positionUpdater.stopUpdating();
            }
        }
    };
    private volatile State state;
    private ScheduledFuture<?> sleepSand;
    private volatile boolean stopAfterCurrentTrack = false;

    public MediaPlayerController(Book book, Context c) {
        L.i(TAG, "new MediaPlayerController with book:" + book);
        this.book = book;
        this.c = c;
        extState = StateManager.getInstance(c);
        prefs = new Prefs(c);
        db = DataBaseHelper.getInstance(c);

        mediaPlayer = new MediaPlayerCompat(c);
        state = State.IDLE;

        prepare();
        positionUpdater = new PositionUpdater(mediaPlayer, c, book);
    }

    private void prepare() {
        if (state != State.IDLE) {
            mediaPlayer.reset();
            state = State.IDLE;
        }
        mediaPlayer.setWakeMode(c, PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE);
        mediaPlayer.setOnCompletionListener(onCompletionListener);
        mediaPlayer.setDataSource(book.getContainingMedia().get(book.getPosition()).getPath());

        mediaPlayer.prepare();
        mediaPlayer.seekTo(book.getTime());
        extState.setTime(book.getTime());
        extState.setPosition(book.getPosition());
        setPlaybackSpeed();
        state = State.PREPARED;
    }

    public void release() {
        L.i(TAG, "release called");
        lock.lock();
        try {
            mediaPlayer.release();
            positionUpdater.stopUpdating();

            extState.setState(PlayerStates.STOPPED);
            extState.setSleepTimerActive(false);
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

                    positionUpdater.startUpdating();

                    extState.setState(PlayerStates.PLAYING);
                    state = State.STARTED;
                    break;
                case DEAD:
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
                extState.setSleepTimerActive(false);
            } else {
                L.i(TAG, "preparing new sleepsand");
                int minutes = prefs.getSleepTime();
                stopAfterCurrentTrack = prefs.stopAfterCurrentTrack();
                extState.setSleepTimerActive(true);

                sleepSand = sandMan.schedule(new Runnable() {
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

    public void changeTime(int time) {
        lock.lock();
        try {
            //{Prepared, Started, Paused, PlaybackCompleted}
            switch (state) {
                case PREPARED:
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    mediaPlayer.seekTo(time);
                    extState.setTime(time);
                    book.setTime(time);
                    db.updateBook(book);
                    break;
                default:
                    L.e(TAG, "changeTime called in illegal state:" + state);
                    break;
            }

        } finally {
            lock.unlock();
        }
    }

    //{Started, Paused, PlaybackCompleted}
    public void pause() {
        lock.lock();
        try {
            switch (state) {
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    mediaPlayer.pause();
                    positionUpdater.stopUpdating();

                    extState.setState(PlayerStates.PAUSED);
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

    public Book getBook() {
        return book;
    }

    public void setPlaybackSpeed() {
        lock.lock();
        try {
            if (state != State.DEAD) {
                mediaPlayer.setPlaybackSpeed(prefs.getPlaybackSpeed());
            } else {
                L.e(TAG, "setPlaybackSpeed called in illegal state: " + state);
            }
        } finally {
            lock.unlock();
        }
    }

    public void changeBookPosition(int position) {
        lock.lock();
        try {
            boolean wasPlaying = (state == State.STARTED);

            mediaPlayer.reset();
            state = State.IDLE;

            book.setPosition(position);
            book.setTime(0);
            db.updateBook(book);

            prepare();

            if (wasPlaying) {
                mediaPlayer.start();

                state = State.STARTED;
                extState.setState(PlayerStates.PLAYING);
            } else {
                state = State.PREPARED;
                extState.setState(PlayerStates.PAUSED);
            }
            extState.setPosition(position);
        } finally {
            lock.unlock();
        }
    }

    public void skip(Direction direction) {
        lock.lock();
        try {
            switch (state) {
                case PREPARED:
                case STARTED:
                case PAUSED:
                case PLAYBACK_COMPLETED:
                    int delta = prefs.getSeekTime() * 1000;
                    int duration = mediaPlayer.getDuration();
                    int intendedPosition = mediaPlayer.getCurrentPosition() +
                            (direction == Direction.BACKWARD ? -delta : delta);
                    if (intendedPosition > duration) {
                        next();
                    } else {
                        if (intendedPosition < 0) {
                            intendedPosition = 0;
                        }
                        mediaPlayer.seekTo(intendedPosition);
                        extState.setTime(intendedPosition);
                        book.setTime(intendedPosition);
                        db.updateBook(book);
                    }
                    break;
                default:
                    L.e(TAG, "skip called in illegal state: " + state);
                    break;
            }

        } finally {
            lock.unlock();
        }
    }

    public void next() {
        lock.lock();
        try {
            int possibleNewPosition = book.getPosition() + 1;
            if (possibleNewPosition < book.getContainingMedia().size()) {
                changeBookPosition(possibleNewPosition);
            } else {
                L.e(TAG, "Next will be dumped. reached last file.");
            }
        } finally {
            lock.unlock();
        }
    }

    public void previous() {
        lock.lock();
        try {
            if (mediaPlayer.getCurrentPosition() > 2000 || book.getPosition() == 0) {
                mediaPlayer.seekTo(0);
                extState.setTime(0);
                book.setTime(0);
                db.updateBook(book);
            } else {
                int newPosition = book.getPosition() - 1;
                changeBookPosition(newPosition);
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

package de.ph1b.audiobook.utils;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.WidgetUpdateService;

public class BaseApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static final String TAG = BaseApplication.class.getSimpleName();
    private final CopyOnWriteArrayList<OnBookAddedListener> onBookAddedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnBookDeletedListener> onBookDeletedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnPositionChangedListener> onPositionChangedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnSleepStateChangedListener> onSleepStateChangedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnPlayStateChangedListener> onPlayStateChangedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnCurrentBookChangedListener> onCurrentBookChangedListeners = new CopyOnWriteArrayList<>();
    private final Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    private ArrayList<Book> allBooks;
    private Book currentBook = null;
    private PlayState currentState = PlayState.STOPPED;
    private PrefsManager prefs;
    private boolean sleepTimerActive = false;

    public void addOnBookAddedListener(OnBookAddedListener listener) {
        onBookAddedListeners.add(listener);
    }

    public void removeOnBookAddedListener(OnBookAddedListener listener) {
        onBookAddedListeners.remove(listener);
    }

    public PlayState getPlayState() {
        return currentState;
    }

    public void setPlayState(BaseApplication.PlayState playState) {
        L.v(TAG, "setPlayState to: " + playState);
        currentState = playState;
        for (OnPlayStateChangedListener l : onPlayStateChangedListeners) {
            l.onPlayStateChanged(currentState);
        }
        startService(new Intent(this, WidgetUpdateService.class));
    }

    public void addOnPlayStateChangedListener(OnPlayStateChangedListener onPlayStateChangedListener) {
        onPlayStateChangedListeners.add(onPlayStateChangedListener);
    }

    public void removeOnPlayStateChangedListener(OnPlayStateChangedListener onPlayStateChangedListener) {
        onPlayStateChangedListeners.remove(onPlayStateChangedListener);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler(this);
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = new PrefsManager(this);
        DataBaseHelper db = DataBaseHelper.getInstance(this);
        allBooks = db.getAllBooks();

        for (Book b : allBooks) {
            if (b.getId() == prefs.getCurrentBookId()) {
                currentBook = b;
                break;
            }
        }
    }

    public ArrayList<Book> getAllBooks() {
        return allBooks;
    }

    public Book getCurrentBook() {
        if (currentBook == null) {
            for (Book b : allBooks) {
                if (b.getId() == prefs.getCurrentBookId()) {
                    currentBook = b;
                }
            }
            if (currentBook == null && allBooks.size() > 0) {
                currentBook = allBooks.get(0);
            }
        }

        return currentBook;
    }

    public void setCurrentBook(Book book) {
        this.currentBook = book;
        for (OnCurrentBookChangedListener l : onCurrentBookChangedListeners) {
            l.onCurrentBookChanged(currentBook);
        }
    }

    public void addOnCurrentBookChangedListener(OnCurrentBookChangedListener listener) {
        onCurrentBookChangedListeners.add(listener);
    }

    public void removeOnCurrentBookChangedListener(OnCurrentBookChangedListener listener) {
        onCurrentBookChangedListeners.remove(listener);
    }

    public void notifyBookDeleted() {
        for (OnBookDeletedListener l : onBookDeletedListeners) {
            l.onBookDeleted();
        }
    }

    public void addOnSleepStateChangedListener(OnSleepStateChangedListener listener) {
        onSleepStateChangedListeners.add(listener);
    }

    public void removeOnSleepStateChangedListener(OnSleepStateChangedListener listener) {
        onSleepStateChangedListeners.remove(listener);
    }

    public void addOnPositionChangedListener(OnPositionChangedListener listener) {
        onPositionChangedListeners.add(listener);
    }

    public void removeOnPositionChangedListener(OnPositionChangedListener listener) {
        onPositionChangedListeners.remove(listener);
    }

    public void notifyPositionChanged() {
        for (OnPositionChangedListener l : onPositionChangedListeners) {
            l.onPositionChanged();
        }
    }

    public void notifyBookAdded() {
        for (OnBookAddedListener l : onBookAddedListeners) {
            l.onBookAdded();
        }
    }

    public boolean isSleepTimerActive() {
        return sleepTimerActive;
    }

    public void setSleepTimerActive(boolean active) {
        this.sleepTimerActive = active;
        for (OnSleepStateChangedListener l : onSleepStateChangedListeners) {
            l.onSleepStateChanged(active);
        }
    }

    public void addOnBookDeletedListener(OnBookDeletedListener listener) {
        onBookDeletedListeners.add(listener);
    }

    public void removeOnBookDeletedListener(OnBookDeletedListener listener) {
        onBookDeletedListeners.remove(listener);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String stackTrace = Log.getStackTraceString(ex);
        String time = new Date(System.currentTimeMillis()).toString();
        String message = ex.getMessage();
        String report = "occured_at\n" + time + "\n\n" +
                "message\n" + message + "\n\n" +
                "stacktrace\n" + stackTrace;
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "woitaschek@gmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bugreport");
        emailIntent.putExtra(Intent.EXTRA_TEXT, report);
        Intent startClientIntent = Intent.createChooser(emailIntent, "Sending mail...");
        startClientIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getApplicationContext().startActivity(startClientIntent);
        defaultUEH.uncaughtException(thread, ex);
    }

    public enum PlayState {
        PLAYING,
        PAUSED,
        STOPPED,
    }

    public interface OnBookAddedListener {
        public void onBookAdded();
    }

    public interface OnCurrentBookChangedListener {
        public void onCurrentBookChanged(Book book);
    }

    public interface OnSleepStateChangedListener {
        public void onSleepStateChanged(boolean active);
    }

    public interface OnPositionChangedListener {
        public void onPositionChanged();
    }

    public interface OnPlayStateChangedListener {
        public void onPlayStateChanged(PlayState state);
    }

    public interface OnBookDeletedListener {
        public void onBookDeleted();
    }
}
package de.ph1b.audiobook.utils;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.WidgetUpdateService;

@ThreadSafe
public class BaseApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static final String TAG = BaseApplication.class.getSimpleName();
    public final ReentrantLock bookLock = new ReentrantLock();
    @GuardedBy("bookLock")
    private final ArrayList<Book> allBooks = new ArrayList<>();
    private final CopyOnWriteArrayList<OnBookAddedListener> onBookAddedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnBookDeletedListener> onBookDeletedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnPositionChangedListener> onPositionChangedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnSleepStateChangedListener> onSleepStateChangedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnPlayStateChangedListener> onPlayStateChangedListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnCurrentBookChangedListener> onCurrentBookChangedListeners = new CopyOnWriteArrayList<>();
    private final Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    private final CopyOnWriteArrayList<OnScannerStateChangedListener> onScannerStateChangedListeners = new CopyOnWriteArrayList<>();
    private DataBaseHelper db;
    @GuardedBy("bookLock")
    private Book currentBook = null;
    private volatile PlayState currentState = PlayState.STOPPED;
    private PrefsManager prefs;
    private boolean sleepTimerActive = false;
    private volatile boolean scannerActive = false;

    public void addOnScannerStateChangedListener(OnScannerStateChangedListener listener) {
        onScannerStateChangedListeners.add(listener);
    }

    public boolean isScannerActive() {
        return scannerActive;
    }

    public void setScannerActive(boolean scannerActive) {
        this.scannerActive = scannerActive;
        for (OnScannerStateChangedListener l : onScannerStateChangedListeners) {
            l.onScannerStateChanged(scannerActive);
        }
    }

    public void removeOnScannerStateChangedListener(OnScannerStateChangedListener listener) {
        onScannerStateChangedListeners.remove(listener);
    }

    public void addOnBookAddedListener(OnBookAddedListener listener) {
        onBookAddedListeners.add(listener);
    }

    public void addBook(Book book) {
        bookLock.lock();
        try {
            db.addBook(book);
            allBooks.add(book);
            for (OnBookAddedListener l : onBookAddedListeners) {
                l.onBookAdded();
            }
        } finally {
            bookLock.unlock();
        }
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
        db = DataBaseHelper.getInstance(this);

        bookLock.lock();
        try {
            allBooks.addAll(db.getAllBooks());
            for (Book b : allBooks) {
                if (b.getId() == prefs.getCurrentBookId()) {
                    currentBook = b;
                    break;
                }
            }
        } finally {
            bookLock.unlock();
        }
    }

    public ArrayList<Book> getAllBooks() {
        bookLock.lock();
        try {
            return allBooks;
        } finally {
            bookLock.unlock();
        }
    }

    @Nullable
    public Book getCurrentBook() {
        bookLock.lock();
        try {
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
        } finally {
            bookLock.unlock();
        }
    }

    public void setCurrentBook(Book book) {
        bookLock.lock();
        try {
            this.currentBook = book;
            for (OnCurrentBookChangedListener l : onCurrentBookChangedListeners) {
                l.onCurrentBookChanged(currentBook);
            }
        } finally {
            bookLock.unlock();
        }
    }

    public void deleteBook(Book book) {
        bookLock.lock();
        try {
            allBooks.remove(book);
            db.deleteBook(book);
            for (OnBookDeletedListener l : onBookDeletedListeners) {
                l.onBookDeleted();
            }
        } finally {
            bookLock.unlock();
        }
    }

    public void addOnCurrentBookChangedListener(OnCurrentBookChangedListener listener) {
        onCurrentBookChangedListeners.add(listener);
    }

    public void removeOnCurrentBookChangedListener(OnCurrentBookChangedListener listener) {
        onCurrentBookChangedListeners.remove(listener);
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
        String report = "occurred_at\n" + time + "\n\n" +
                "message\n" + message + "\n\n" +
                "stacktrace\n" + stackTrace;

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "woitaschek@gmail.com", null));
        //emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bugreport Material Audiobook Player");
        emailIntent.putExtra(Intent.EXTRA_TEXT, report);
        Intent startClientIntent = Intent.createChooser(emailIntent, "Sending MAP Bugreport...");
        startClientIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            getApplicationContext().startActivity(startClientIntent);
        } catch (ActivityNotFoundException ignored) {
        }

        defaultUEH.uncaughtException(thread, ex);
    }

    public enum PlayState {
        PLAYING,
        PAUSED,
        STOPPED,
    }

    public interface OnScannerStateChangedListener {
        public void onScannerStateChanged(boolean active);
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
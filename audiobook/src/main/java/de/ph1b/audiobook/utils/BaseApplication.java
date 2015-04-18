package de.ph1b.audiobook.utils;

import android.app.Application;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.WidgetUpdateService;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://acra-63e870.smileupps.com/acra-material/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "defaultreporter",
        formUriBasicAuthPassword = "KA0Kc8h4dV4lCZBz")
@ThreadSafe
public class BaseApplication extends Application {
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
    private final CopyOnWriteArrayList<OnScannerStateChangedListener> onScannerStateChangedListeners = new CopyOnWriteArrayList<>();
    private DataBaseHelper db;
    @GuardedBy("bookLock")
    private Book currentBook = null;
    private volatile PlayState currentState = PlayState.STOPPED;
    private PrefsManager prefs;
    private volatile boolean sleepTimerActive = false;
    private volatile boolean scannerActive = false;
    private BookAdder bookAdder;

    public Book getBook(long id) {
        bookLock.lock();
        try {
            for (Book b : allBooks) {
                if (b.getId() == id) {
                    return b;
                }
            }
        } finally {
            bookLock.unlock();
        }
        throw new AssertionError("Get book with id=" + id + " did not find a book");
    }

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
            Collections.sort(allBooks);
            int position = allBooks.indexOf(book);
            for (OnBookAddedListener l : onBookAddedListeners) {
                l.onBookAdded(position);
            }
        } finally {
            bookLock.unlock();
        }
    }

    public void removeOnBookAddedListener(OnBookAddedListener listener) {
        onBookAddedListeners.remove(listener);
    }

    public void scanForFiles(final boolean interrupting) {
        bookAdder.scanForFiles(interrupting);
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

        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = new PrefsManager(this);
        db = DataBaseHelper.getInstance(this);
        bookAdder = new BookAdder(this);

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

        bookAdder.scanForFiles(true);
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
            if (this.currentBook != book) {
                this.currentBook = book;
                for (OnCurrentBookChangedListener l : onCurrentBookChangedListeners) {
                    l.onCurrentBookChanged(currentBook);
                }
            }
        } finally {
            bookLock.unlock();
        }
    }

    public void deleteBook(Book book) {
        bookLock.lock();
        try {
            db.deleteBook(book);
            int position = allBooks.indexOf(book);
            allBooks.remove(book);
            for (OnBookDeletedListener l : onBookDeletedListeners) {
                l.onBookDeleted(position);
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

    public void notifyPositionChanged(boolean fileChanged) {
        for (OnPositionChangedListener l : onPositionChangedListeners) {
            l.onPositionChanged(fileChanged);
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

    public enum PlayState {
        PLAYING,
        PAUSED,
        STOPPED,
    }

    public interface OnScannerStateChangedListener {
        public void onScannerStateChanged(boolean active);
    }

    public interface OnBookAddedListener {
        public void onBookAdded(int position);
    }

    public interface OnCurrentBookChangedListener {
        public void onCurrentBookChanged(Book book);
    }

    public interface OnSleepStateChangedListener {
        public void onSleepStateChanged(boolean active);
    }

    public interface OnPositionChangedListener {
        public void onPositionChanged(boolean fileChanged);
    }

    public interface OnPlayStateChangedListener {
        public void onPlayStateChanged(PlayState state);
    }

    public interface OnBookDeletedListener {
        public void onBookDeleted(int position);
    }
}
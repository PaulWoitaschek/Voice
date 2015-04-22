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
    private final ReentrantLock bookLock = new ReentrantLock();
    @GuardedBy("bookLock")
    private final ArrayList<Book> allBooks = new ArrayList<>();
    private final CopyOnWriteArrayList<OnBooksChangedListener> onBooksChangedListeners = new CopyOnWriteArrayList<>();
    private DataBaseHelper db;
    @GuardedBy("bookLock")
    private Book currentBook = null;
    private volatile PlayState currentState = PlayState.STOPPED;
    private PrefsManager prefs;
    private volatile boolean sleepTimerActive = false;
    private volatile boolean scannerActive = false;
    private BookAdder bookAdder;

    public Book getBook(long id) {
        L.v(TAG, "getBook acquiring lock");
        bookLock.lock();
        L.v(TAG, "getBook acquired lock");
        try {
            for (Book b : allBooks) {
                if (b.getId() == id) {
                    return b;
                }
            }
        } finally {
            L.v(TAG, "getBook released lock");
            bookLock.unlock();
        }
        throw new AssertionError("Get book with id=" + id + " did not find a book");
    }

    public boolean isScannerActive() {
        return scannerActive;
    }

    public void setScannerActive(boolean scannerActive) {
        this.scannerActive = scannerActive;
        for (OnBooksChangedListener l : onBooksChangedListeners) {
            l.onScannerStateChanged(scannerActive);
        }
    }

    public void addBook(Book book) {
        L.v(TAG, "addBook acquiring lock...");
        bookLock.lock();
        L.v(TAG, "addBook acquired lock...");
        try {
            db.addBook(book);
            allBooks.add(book);
            Collections.sort(allBooks);
            int position = allBooks.indexOf(book);
            for (OnBooksChangedListener l : onBooksChangedListeners) {
                l.onBookAdded(position);
            }
        } finally {
            L.v(TAG, "addBook released lock...");
            bookLock.unlock();
        }
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
        for (OnBooksChangedListener l : onBooksChangedListeners) {
            l.onPlayStateChanged(currentState);
        }
        startService(new Intent(this, WidgetUpdateService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = new PrefsManager(this);
        db = DataBaseHelper.getInstance(this);
        bookAdder = new BookAdder(this);

        allBooks.addAll(db.getAllBooks());
        for (Book b : allBooks) {
            if (b.getId() == prefs.getCurrentBookId()) {
                currentBook = b;
                break;
            }
        }

        bookAdder.scanForFiles(true);
    }

    public ArrayList<Book> getAllBooks() {
        L.v(TAG, "getAllBooks acquiring lock...");
        bookLock.lock();
        L.v(TAG, "getAllBooks acquired lock...");
        try {
            return allBooks;
        } finally {
            L.v(TAG, "getAllBooks releasing lock...");
            bookLock.unlock();
        }
    }

    @Nullable
    public Book getCurrentBook() {
        L.v(TAG, "getCurrentBook acquiring lock");
        bookLock.lock();
        L.v(TAG, "getCurrentBook acquired lock");
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
            L.v(TAG, "getCurrentBook releasing lock");
            bookLock.unlock();
        }
    }

    public void setCurrentBook(Book book) {
        L.v(TAG, "setCurrentBook acquiring lock");
        bookLock.lock();
        L.v(TAG, "setCurrentBook acquired lock");
        try {
            if (this.currentBook != book) {
                this.currentBook = book;
                for (OnBooksChangedListener l : onBooksChangedListeners) {
                    l.onCurrentBookChanged(currentBook);
                }
            }
        } finally {
            L.v(TAG, "setCurrentBook releasing lock");
            bookLock.unlock();
        }
    }

    public void deleteBook(Book book) {
        L.v(TAG, "deleteBook acquiring lock");
        bookLock.lock();
        L.v(TAG, "deleteBook acquired lock");
        try {
            db.deleteBook(book);
            int position = allBooks.indexOf(book);
            allBooks.remove(book);
            for (OnBooksChangedListener l : onBooksChangedListeners) {
                l.onBookDeleted(position);
            }
        } finally {
            L.v(TAG, "deleteBook releasing lock");
            bookLock.unlock();
        }
    }

    public void notifyPositionChanged(boolean fileChanged) {
        for (OnBooksChangedListener l : onBooksChangedListeners) {
            l.onPositionChanged(fileChanged);
        }
    }

    public boolean isSleepTimerActive() {
        return sleepTimerActive;
    }

    public void setSleepTimerActive(boolean active) {
        this.sleepTimerActive = active;
        for (OnBooksChangedListener l : onBooksChangedListeners) {
            l.onSleepStateChanged(active);
        }
    }

    public void removeOnBooksChangedListener(OnBooksChangedListener listener) {
        onBooksChangedListeners.remove(listener);
    }

    public void addOnBooksChangedListener(OnBooksChangedListener listener) {
        onBooksChangedListeners.add(listener);
    }

    public enum PlayState {
        PLAYING,
        PAUSED,
        STOPPED,
    }


    public interface OnBooksChangedListener {
        public void onBookDeleted(int position);

        public void onPlayStateChanged(PlayState state);

        public void onPositionChanged(boolean fileChanged);

        public void onSleepStateChanged(boolean active);

        public void onCurrentBookChanged(Book book);

        public void onBookAdded(int position);

        public void onScannerStateChanged(boolean active);

    }
}
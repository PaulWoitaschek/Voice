package de.ph1b.audiobook.utils;

import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;


/**
 * Class for communicating on different events through {@link LocalBroadcastManager}.
 */
@ThreadSafe
public class Communication {

    private static final Communication INSTANCE = new Communication();
    private final List<OnBookSetChangedListener> onBookSetChangedListeners = new ArrayList<>();
    private final List<OnSleepStateChangedListener> onSleepStateChangedListeners = new ArrayList<>();
    private final List<OnBookContentChangedListener> onBookContentChangedListeners = new ArrayList<>();
    private final List<OnPlayStateChangedListener> onPlayStateChangedListeners = new ArrayList<>();
    private final List<OnScannerStateChangedListener> onScannerStateChangedListeners = new ArrayList<>();
    private final List<OnCurrentBookIdChangedListener> onCurrentBookIdChangedListeners = new ArrayList<>();

    private Communication() {
    }

    public static Communication getInstance() {
        return INSTANCE;
    }


    /**
     * Notifies the listeners that the sleep-timer has either been started or cancelled.
     *
     * @see MediaPlayerController#sleepSandActive()
     */
    public synchronized void sleepStateChanged() {
        for (OnSleepStateChangedListener listener : onSleepStateChangedListeners) {
            listener.onSleepStateChanged();
        }
    }

    public synchronized void addOnSleepStateChangedListener(OnSleepStateChangedListener onSleepStateChangedListener) {
        onSleepStateChangedListeners.add(onSleepStateChangedListener);
    }

    public synchronized void removeOnSleepStateChangedListener(OnSleepStateChangedListener onSleepStateChangedListener) {
        onSleepStateChangedListeners.remove(onSleepStateChangedListener);
    }

    /**
     * Sends a broadcast signaling that the {@link de.ph1b.audiobook.model.BookAdder} has been
     * either started or stopped.
     *
     * @see de.ph1b.audiobook.model.BookAdder#scannerActive
     */
    public synchronized void sendScannerStateChanged() {
        for (OnScannerStateChangedListener onScannerStateChangedListener : onScannerStateChangedListeners) {
            onScannerStateChangedListener.onScannerStateChanged();
        }
    }

    public synchronized void addOnScannerStateChangedListener(OnScannerStateChangedListener onScannerStateChangedListener) {
        onScannerStateChangedListeners.add(onScannerStateChangedListener);
    }

    public synchronized void removeOnScannerStateChangedListener(OnScannerStateChangedListener onScannerStateChangedListener) {
        onScannerStateChangedListeners.remove(onScannerStateChangedListener);
    }

    /**
     * Sends a broadcast signaling that the current book that should be playing has been changed
     *
     * @param oldId The old {@link de.ph1b.audiobook.model.Book#id}
     */
    public synchronized void sendCurrentBookChanged(long oldId) {
        for (OnCurrentBookIdChangedListener onCurrentBookIdChangedListener : onCurrentBookIdChangedListeners) {
            onCurrentBookIdChangedListener.onCurrentBookIdChanged(oldId);
        }
    }

    public synchronized void addOnCurrentBookIdChangedListener(OnCurrentBookIdChangedListener onCurrentBookIdChangedListener) {
        onCurrentBookIdChangedListeners.add(onCurrentBookIdChangedListener);
    }

    public synchronized void removeOnCurrentBookIdChangedListener(OnCurrentBookIdChangedListener onCurrentBookIdChangedListener) {
        onCurrentBookIdChangedListeners.remove(onCurrentBookIdChangedListener);
    }

    /**
     * Sends a broadcast signaling that the
     * {@link de.ph1b.audiobook.mediaplayer.MediaPlayerController.PlayState} has changed.
     */
    public synchronized void playStateChanged() {
        for (OnPlayStateChangedListener onPlayStateChangedListener : onPlayStateChangedListeners) {
            onPlayStateChangedListener.onPlayStateChanged();
        }
    }

    public synchronized void addOnPlayStateChangedListener(OnPlayStateChangedListener onPlayStateChangedListener) {
        onPlayStateChangedListeners.add(onPlayStateChangedListener);
    }

    public synchronized void removeOnPlayStateChangedListener(OnPlayStateChangedListener onPlayStateChangedListener) {
        onPlayStateChangedListeners.remove(onPlayStateChangedListener);
    }

    /**
     * Notifies listeners, that the whole set of Books has changed.
     *
     * @param allBooks The whole book set
     */
    public synchronized void bookSetChanged(List<Book> allBooks) {
        for (OnBookSetChangedListener onBookSetChangedListener : onBookSetChangedListeners) {
            List<Book> copyBooks = new ArrayList<>();
            for (Book b : allBooks) {
                copyBooks.add(new Book(b));
            }
            onBookSetChangedListener.onBookSetChanged(copyBooks);
        }
    }

    public synchronized void addOnBookSetChangedListener(OnBookSetChangedListener onBookSetChangedListener) {
        onBookSetChangedListeners.add(onBookSetChangedListener);
    }

    public synchronized void removeOnBookSetChangedListener(OnBookSetChangedListener onBookSetChangedListener) {
        onBookSetChangedListeners.remove(onBookSetChangedListener);
    }

    public synchronized void addOnBookContentChangedListener(OnBookContentChangedListener onBookContentChangedListener) {
        onBookContentChangedListeners.add(onBookContentChangedListener);
    }

    public synchronized void removeOnBookContentChangedListener(OnBookContentChangedListener onBookContentChangedListener) {
        onBookContentChangedListeners.remove(onBookContentChangedListener);
    }

    /**
     * Sends a broadcast signaling that a certain book has changed.
     *
     * @param book The book that has changed
     */
    public synchronized void sendBookContentChanged(@NonNull Book book) {
        for (OnBookContentChangedListener onBookContentChangedListener : onBookContentChangedListeners) {
            // copy constructor for immutabliity
            onBookContentChangedListener.onBookContentChanged(new Book(book));
        }
    }

    public interface OnCurrentBookIdChangedListener {
        void onCurrentBookIdChanged(long oldId);
    }

    public interface OnScannerStateChangedListener {
        void onScannerStateChanged();
    }

    public interface OnPlayStateChangedListener {
        void onPlayStateChanged();
    }

    public interface OnBookContentChangedListener {
        void onBookContentChanged(@NonNull Book book);
    }

    public interface OnSleepStateChangedListener {
        void onSleepStateChanged();
    }

    public interface OnBookSetChangedListener {
        void onBookSetChanged(@NonNull List<Book> activeBooks);
    }
}

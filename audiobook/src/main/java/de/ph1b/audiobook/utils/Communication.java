package de.ph1b.audiobook.utils;

import android.support.v4.content.LocalBroadcastManager;

import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;

import de.ph1b.audiobook.mediaplayer.MediaPlayerController;


/**
 * Class for communicating on different events through {@link LocalBroadcastManager}.
 */
@ThreadSafe
public class Communication {

    private static Communication instance;
    private final ArrayList<OnBookSetChangedListener> onBookSetChangedListeners = new ArrayList<>();
    private final ArrayList<OnSleepStateChangedListener> onSleepStateChangedListeners = new ArrayList<>();
    private final ArrayList<OnCoverChangedListener> onCoverChangedListeners = new ArrayList<>();
    private final ArrayList<OnBookContentChangedListener> onBookContentChangedListeners = new ArrayList<>();
    private final ArrayList<OnPlayStateChangedListener> onPlayStateChangedListeners = new ArrayList<>();
    private final ArrayList<OnScannerStateChangedListener> onScannerStateChangedListeners = new ArrayList<>();
    private final ArrayList<OnCurrentBookIdChangedListener> onCurrentBookIdChangedListeners = new ArrayList<>();

    private Communication() {
    }

    public static synchronized Communication getInstance() {
        if (instance == null) {
            instance = new Communication();
        }
        return instance;
    }

    /**
     * Sends a broadcast indicating that a cover for a certain Book has changed
     *
     * @param bookId The book ID for which the cover has changed
     */
    public synchronized void sendCoverChanged(long bookId) {
        for (OnCoverChangedListener onCoverChangedListener : onCoverChangedListeners) {
            onCoverChangedListener.onCoverChanged(bookId);
        }
    }

    public void addOnCoverChangedListener(OnCoverChangedListener onCoverChangedListener) {
        onCoverChangedListeners.add(onCoverChangedListener);
    }

    public void removeOnCoverChangedListener(OnCoverChangedListener onCoverChangedListener) {
        onCoverChangedListeners.remove(onCoverChangedListener);
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
     */
    public synchronized void bookSetChanged() {
        for (OnBookSetChangedListener onBookSetChangedListener : onBookSetChangedListeners) {
            onBookSetChangedListener.onBookSetChanged();
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
     * @param bookId THe book id for the book that has changed.
     */
    public synchronized void sendBookContentChanged(long bookId) {
        for (OnBookContentChangedListener onBookContentChangedListener : onBookContentChangedListeners) {
            onBookContentChangedListener.onBookContentChanged(bookId);
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
        void onBookContentChanged(long bookId);
    }


    public interface OnCoverChangedListener {
        void onCoverChanged(long bookId);
    }

    public interface OnSleepStateChangedListener {
        void onSleepStateChanged();
    }

    public interface OnBookSetChangedListener {
        void onBookSetChanged();
    }
}

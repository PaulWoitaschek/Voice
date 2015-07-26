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
    private final List<BookCommunication> listeners = new ArrayList<>();

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
        for (BookCommunication listener : listeners) {
            listener.onSleepStateChanged();
        }
    }


    /**
     * Sends a broadcast signaling that the {@link de.ph1b.audiobook.model.BookAdder} has been
     * either started or stopped.
     *
     * @see de.ph1b.audiobook.model.BookAdder#scannerActive
     */
    public synchronized void sendScannerStateChanged() {
        for (BookCommunication listener : listeners) {
            listener.onScannerStateChanged();
        }
    }


    /**
     * Sends a broadcast signaling that the current book that should be playing has been changed
     *
     * @param oldId The old {@link de.ph1b.audiobook.model.Book#id}
     */
    public synchronized void sendCurrentBookChanged(long oldId) {
        for (BookCommunication listener : listeners) {
            listener.onCurrentBookIdChanged(oldId);
        }
    }


    /**
     * Sends a broadcast signaling that the
     * {@link de.ph1b.audiobook.mediaplayer.MediaPlayerController.PlayState} has changed.
     */
    public synchronized void playStateChanged() {
        for (BookCommunication listener : listeners) {
            listener.onPlayStateChanged();
        }
    }


    /**
     * Notifies listeners, that the whole set of Books has changed.
     *
     * @param allBooks The whole book set
     */
    public synchronized void bookSetChanged(List<Book> allBooks) {
        for (BookCommunication listener : listeners) {
            List<Book> copyBooks = new ArrayList<>();
            for (Book b : allBooks) {
                copyBooks.add(new Book(b));
            }
            listener.onBookSetChanged(copyBooks);
        }
    }


    public synchronized void removeBookCommunicationListener(BookCommunication listener) {
        listeners.remove(listener);
    }

    public synchronized void addBookCommunicationListener(BookCommunication listener) {
        listeners.add(listener);
    }

    /**
     * Sends a broadcast signaling that a certain book has changed.
     *
     * @param book The book that has changed
     */
    public synchronized void sendBookContentChanged(@NonNull Book book) {
        for (BookCommunication listener : listeners) {
            // copy constructor for immutabliity
            listener.onBookContentChanged(new Book(book));
        }
    }

    public interface BookCommunication {


        void onCurrentBookIdChanged(long oldId);

        void onScannerStateChanged();


        void onPlayStateChanged();


        void onBookContentChanged(@NonNull Book book);


        void onSleepStateChanged();


        void onBookSetChanged(@NonNull List<Book> activeBooks);
    }

    public static class SimpleBookCommunication implements BookCommunication {

        @Override
        public void onCurrentBookIdChanged(long oldId) {

        }

        @Override
        public void onScannerStateChanged() {

        }

        @Override
        public void onPlayStateChanged() {

        }

        @Override
        public void onBookContentChanged(@NonNull Book book) {

        }

        @Override
        public void onSleepStateChanged() {

        }

        @Override
        public void onBookSetChanged(@NonNull List<Book> activeBooks) {

        }
    }
}

package de.ph1b.audiobook.utils;

import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;


/**
 * Class for communicating on different events through {@link LocalBroadcastManager}.
 */
@Singleton
public class Communication {

    private final List<BookCommunication> listeners = new ArrayList<>(10);
    private final Executor executor = Executors.newSingleThreadScheduledExecutor();

    @Inject
    public Communication() {
    }


    /**
     * Notifies the listeners that the sleep-timer has either been started or cancelled.
     *
     * @see MediaPlayerController#sleepSandActive()
     */
    public synchronized void sleepStateChanged() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (BookCommunication listener : listeners) {
                    listener.onSleepStateChanged();
                }
            }
        });
    }


    /**
     * Sends a broadcast signaling that the {@link de.ph1b.audiobook.model.BookAdder} has been
     * either started or stopped.
     *
     * @see de.ph1b.audiobook.model.BookAdder#scannerActive
     */
    public synchronized void sendScannerStateChanged() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (BookCommunication listener : listeners) {
                    listener.onScannerStateChanged();
                }
            }
        });
    }


    /**
     * Sends a broadcast signaling that the current book that should be playing has been changed
     *
     * @param oldId The old {@link de.ph1b.audiobook.model.Book#id}
     */
    public synchronized void sendCurrentBookChanged(final long oldId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (BookCommunication listener : listeners) {
                    listener.onCurrentBookIdChanged(oldId);
                }
            }
        });
    }


    /**
     * Sends a broadcast signaling that the
     * {@link de.ph1b.audiobook.mediaplayer.MediaPlayerController.PlayState} has changed.
     */
    public synchronized void playStateChanged() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (BookCommunication listener : listeners) {
                    listener.onPlayStateChanged();
                }
            }
        });
    }


    /**
     * Notifies listeners, that the whole set of Books has changed.
     *
     * @param allBooks The whole book set
     */
    public synchronized void bookSetChanged(final List<Book> allBooks) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (BookCommunication listener : listeners) {
                    List<Book> copyBooks = new ArrayList<>(allBooks.size());
                    for (Book b : allBooks) {
                        copyBooks.add(Book.of(b));
                    }
                    listener.onBookSetChanged(copyBooks);
                }
            }
        });
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
    public synchronized void sendBookContentChanged(@NonNull final Book book) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (BookCommunication listener : listeners) {
                    // copy constructor for immutabliity
                    listener.onBookContentChanged(Book.of(book));
                }
            }
        });
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

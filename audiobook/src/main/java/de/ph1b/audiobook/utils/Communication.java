package de.ph1b.audiobook.utils;

import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ph1b.audiobook.mediaplayer.MediaPlayerController;


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
        executor.execute(() -> {
            for (BookCommunication listener : listeners) {
                listener.onSleepStateChanged();
            }
        });
    }


    /**
     * Sends a broadcast signaling that the current book that should be playing has been changed
     *
     * @param oldId The old {@link de.ph1b.audiobook.model.Book#id}
     */
    public synchronized void sendCurrentBookChanged(final long oldId) {
        executor.execute(() -> {
            for (BookCommunication listener : listeners) {
                listener.onCurrentBookIdChanged(oldId);
            }
        });
    }

    public synchronized void removeBookCommunicationListener(BookCommunication listener) {
        listeners.remove(listener);
    }

    public synchronized void addBookCommunicationListener(BookCommunication listener) {
        listeners.add(listener);
    }

    public interface BookCommunication {

        void onCurrentBookIdChanged(long oldId);

        void onSleepStateChanged();
    }

    public static class SimpleBookCommunication implements BookCommunication {

        @Override
        public void onCurrentBookIdChanged(long oldId) {

        }

        @Override
        public void onSleepStateChanged() {

        }
    }
}

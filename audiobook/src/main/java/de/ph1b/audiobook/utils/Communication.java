package de.ph1b.audiobook.utils;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import de.ph1b.audiobook.mediaplayer.MediaPlayerController;


/**
 * Class for communicating on different events through {@link LocalBroadcastManager}.
 */
public class Communication {

    public static final String CURRENT_BOOK_CHANGED = "currentBookChanged";
    public static final String CURRENT_BOOK_CHANGED_OLD_ID = "currentBookChangedOldId";
    public static final String BOOK_SET_CHANGED = "bookChanged";
    public static final String SLEEP_STATE_CHANGED = "sleepStateChanged";
    public static final String SCANNER_STATE_CHANGED = "scannerStateChanged";
    public static final String PLAY_STATE_CHANGED = "playStateChanged";
    public static final String COVER_CHANGED = "coverChanged";
    public static final String COVER_CHANGED_BOOK_ID = "coverChanged";

    private LocalBroadcastManager bcm;

    public Communication(@NonNull Context c) {
        bcm = LocalBroadcastManager.getInstance(c);
    }

    /**
     * Sends a broadcast indicating that a cover for a certain Book has changed
     *
     * @param bookId The book ID for which the cover has changed
     */
    public void sendCoverChanged(long bookId) {
        Intent intent = new Intent(COVER_CHANGED);
        intent.putExtra(COVER_CHANGED_BOOK_ID, bookId);
        bcm.sendBroadcast(intent);
    }

    /**
     * Sends a broadcast signaling that the sleep-timer has either started or cancelled.
     *
     * @see MediaPlayerController#sleepSandActive()
     */
    public void sendSleepStateChanged() {
        bcm.sendBroadcast(new Intent(SLEEP_STATE_CHANGED));
    }

    /**
     * Sends a broadcast signaling that the {@link de.ph1b.audiobook.model.BookAdder} has been
     * either started or stopped.
     *
     * @see de.ph1b.audiobook.model.BookAdder#scannerActive
     */
    public void sendScannerStateChanged() {
        bcm.sendBroadcast(new Intent(SCANNER_STATE_CHANGED));
    }

    /**
     * Sends a broadcast signaling that the current book that should be playing has been changed
     *
     * @param oldId The old {@link de.ph1b.audiobook.model.Book#id}
     */
    public void sendCurrentBookChanged(long oldId) {
        Intent intent = new Intent(CURRENT_BOOK_CHANGED);
        intent.putExtra(CURRENT_BOOK_CHANGED_OLD_ID, oldId);
        bcm.sendBroadcast(intent);
    }

    /**
     * Sends a broadcast signaling that the
     * {@link de.ph1b.audiobook.mediaplayer.MediaPlayerController.PlayState} has changed.
     */
    public void sendPlayStateChanged() {
        bcm.sendBroadcast(new Intent(PLAY_STATE_CHANGED));
    }

    /**
     * Sends a broadcast signaling that the whole set of Books has been changed.
     */
    public void sendBookSetChanged() {
        bcm.sendBroadcast(new Intent(BOOK_SET_CHANGED));
    }
}

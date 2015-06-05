package de.ph1b.audiobook.utils;

import android.content.Context;
import android.content.Intent;
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

    /**
     * Sends a broadcast indicating that a cover for a certain Book has changed
     *
     * @param c      The context
     * @param bookId The book ID for which the cover has changed
     */
    public static void sendCoverChanged(Context c, long bookId) {
        Intent intent = new Intent(COVER_CHANGED);
        intent.putExtra(COVER_CHANGED_BOOK_ID, bookId);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

    /**
     * Sends a broadcast signaling that the sleep-timer has either started or cancelled.
     *
     * @param c The context
     * @see MediaPlayerController#sleepSandActive()
     */
    public static void sendSleepStateChanged(Context c) {
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(SLEEP_STATE_CHANGED));
    }

    /**
     * Sends a broadcast signaling that the {@link de.ph1b.audiobook.model.BookAdder} has been
     * either started or stopped.
     *
     * @param c The Context
     * @see de.ph1b.audiobook.model.BookAdder#scannerActive
     */
    public static void sendScannerStateChanged(Context c) {
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(SCANNER_STATE_CHANGED));
    }

    /**
     * Sends a broadcast signaling that the current book that should be playing has been changed
     *
     * @param c     The context
     * @param oldId The old {@link de.ph1b.audiobook.model.Book#id}
     */
    public static void sendCurrentBookChanged(Context c, long oldId) {
        Intent intent = new Intent(CURRENT_BOOK_CHANGED);
        intent.putExtra(CURRENT_BOOK_CHANGED_OLD_ID, oldId);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

    /**
     * Sends a broadcast signaling that the
     * {@link de.ph1b.audiobook.mediaplayer.MediaPlayerController.PlayState} has changed.
     *
     * @param c The Context
     */
    public static void sendPlayStateChanged(Context c) {
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(PLAY_STATE_CHANGED));
    }

    /**
     * Sends a broadcast signaling that the whole set of Books has been changed.
     *
     * @param c The Context
     */
    public static void sendBookSetChanged(Context c) {
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(BOOK_SET_CHANGED));
    }
}

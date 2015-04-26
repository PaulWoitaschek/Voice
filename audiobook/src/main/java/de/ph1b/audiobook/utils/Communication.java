package de.ph1b.audiobook.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;


public class Communication {

    public static final String CURRENT_BOOK_CHANGED = "currentBookChanged";
    public static final String CURRENT_BOOK_CHANGED_OLD_ID = "currentBookChangedOldId";
    public static final String BOOK_SET_CHANGED = "bookChanged";
    public static final String SLEEP_STATE_CHANGED = "sleepStateChanged";
    public static final String SCANNER_STATE_CHANGED = "scannerStateChanged";
    public static final String PLAY_STATE_CHANGED = "playStateChanged";
    public static final String COVER_CHANGED = "coverChanged";
    public static final String COVER_CHANGED_BOOK_ID = "coverChanged";

    public static void sendCoverChanged(Context c, long bookId) {
        Intent intent = new Intent(COVER_CHANGED);
        intent.putExtra(COVER_CHANGED_BOOK_ID, bookId);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

    public static void sendSleepStateChanged(Context c) {
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(SLEEP_STATE_CHANGED));
    }

    public static void sendScannerStateChanged(Context c) {
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(SCANNER_STATE_CHANGED));
    }

    public static void sendCurrentBookChanged(Context c, long oldId) {
        Intent intent = new Intent(CURRENT_BOOK_CHANGED);
        intent.putExtra(CURRENT_BOOK_CHANGED_OLD_ID, oldId);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

    public static void sendPlayStateChanged(Context c) {
        LocalBroadcastManager.getInstance(c).sendBroadcast(new Intent(PLAY_STATE_CHANGED));
    }
}

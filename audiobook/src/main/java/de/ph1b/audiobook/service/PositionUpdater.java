package de.ph1b.audiobook.service;


import android.content.Context;
import android.os.Handler;

import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.mediaplayer.MediaPlayerCompat;

public class PositionUpdater {

    private final MediaPlayerCompat mp;
    private final StateManager stateManager;
    private final Handler handler = new Handler();
    private final Book book;
    private final Runnable runner = new Runnable() {
        @Override
        public void run() {
            int pos = mp.getCurrentPosition();
            stateManager.setTime(pos);
            book.setTime(pos);
            db.updateBook(book);
            handler.postDelayed(this, 1000);
        }
    };
    private final DataBaseHelper db;

    public PositionUpdater(MediaPlayerCompat mp, Context c, Book book) {
        this.mp = mp;
        this.stateManager = StateManager.getInstance(c);
        this.book = book;
        this.db = DataBaseHelper.getInstance(c);
    }

    public void startUpdating() {
        handler.removeCallbacks(runner);
        handler.post(runner);
    }

    public void stopUpdating() {
        handler.removeCallbacks(runner);
    }
}
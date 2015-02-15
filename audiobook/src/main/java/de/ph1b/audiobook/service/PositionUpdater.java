package de.ph1b.audiobook.service;


import android.content.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.mediaplayer.MediaPlayerCompat;

public class PositionUpdater {

    private final MediaPlayerCompat mp;
    private final GlobalState globalState;
    private final Book book;

    private final DataBaseHelper db;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture updater = null;

    public PositionUpdater(MediaPlayerCompat mp, Context c, Book book) {
        this.mp = mp;
        this.globalState = GlobalState.getInstance(c);
        this.book = book;
        this.db = DataBaseHelper.getInstance(c);
    }

    private boolean updaterActive() {
        return updater != null && !updater.isCancelled() && !updater.isDone();
    }

    public void startUpdating() {
        if (!updaterActive()) {
            updater = executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    int pos = mp.getCurrentPosition();
                    globalState.setTime(pos);
                    book.setTime(pos);
                    db.updateBook(book);
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    public void stopUpdating() {
        if (updaterActive()) {
            updater.cancel(true);
        }
    }
}
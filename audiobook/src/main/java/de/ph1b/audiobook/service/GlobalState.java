package de.ph1b.audiobook.service;

import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;


public enum GlobalState {

    INSTANCE;

    private static final String TAG = GlobalState.class.getSimpleName();
    private final List<ChangeListener> listeners = new CopyOnWriteArrayList<>(); //to avoid massive synchronization
    private Context c;
    private PlayerStates state = PlayerStates.STOPPED;
    private DataBaseHelper db = null;
    private int time;
    private boolean sleepTimerActive = false;
    private int position;
    private Book book = null;

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        if (this.book == null || !this.book.equals(book)) {
            L.v(TAG, "new book set.");
            L.v(TAG, "oldBook=" + this.book + ", newBook=" + book);
            this.book = book;
            setTime(book.getTime());
            setPosition(book.getPosition());
            for (ChangeListener l : listeners) {
                l.onBookChanged(book);
            }
            updateWidget();
        }
    }

    public void init(Context c) {
        this.c = c.getApplicationContext();
        db = DataBaseHelper.getInstance(c);
        PrefsManager prefs = new PrefsManager(c);

        if (book == null) {
            long bookId = prefs.getCurrentBookId();
            setBook(db.getBook(bookId));
        }
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        L.v(TAG, "setPosition to:" + position);
        this.position = position;
        for (ChangeListener l : listeners) {
            l.onPositionChanged(position);
        }
        updateWidget();
    }

    public PlayerStates getState() {
        return state;
    }

    public void setState(PlayerStates state) {
        L.d(TAG, "setState:" + state);
        this.state = state;
        for (ChangeListener l : listeners) {
            l.onStateChanged(state);
        }
        updateWidget();
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
        for (ChangeListener l : listeners) {
            l.onTimeChanged(time);
        }
    }

    public boolean isSleepTimerActive() {
        return sleepTimerActive;
    }

    public void setSleepTimerActive(boolean sleepTimerActive) {
        this.sleepTimerActive = sleepTimerActive;
        for (ChangeListener l : listeners) {
            l.onSleepTimerSet(sleepTimerActive);
        }
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    private void updateWidget() {
        c.startService(new Intent(c, WidgetUpdateService.class));
    }

    public interface ChangeListener {
        public void onTimeChanged(int time);

        public void onStateChanged(PlayerStates state);

        public void onSleepTimerSet(boolean sleepTimerActive);

        public void onPositionChanged(int position);

        public void onBookChanged(Book book);
    }
}

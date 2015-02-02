package de.ph1b.audiobook.service;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.receiver.WidgetProvider;
import de.ph1b.audiobook.utils.L;


public class StateManager {
    private static final String TAG = "StateManager";
    private static StateManager instance;
    private final List<ChangeListener> listeners = new ArrayList<>();
    private final Context c;
    private PlayerStates state = PlayerStates.STOPPED;
    private int time;
    private boolean sleepTimerActive = false;

    private StateManager(Context c) {
        this.c = c;
    }

    public static synchronized StateManager getInstance(Context c) {
        if (instance == null) {
            instance = new StateManager(c.getApplicationContext());
        }
        return instance;
    }

    public void setPosition(int position) {
        L.v(TAG, "setPosition to:" + position);
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
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);
        int ids[] = appWidgetManager.getAppWidgetIds(new ComponentName(c, WidgetProvider.class));
        Intent intent = new Intent(c, WidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        c.sendBroadcast(intent);
    }

    public interface ChangeListener {
        public void onTimeChanged(int time);

        public void onStateChanged(PlayerStates state);

        public void onSleepTimerSet(boolean sleepTimerActive);

        public void onPositionChanged(int position);
    }
}

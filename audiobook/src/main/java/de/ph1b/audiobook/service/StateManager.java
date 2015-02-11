package de.ph1b.audiobook.service;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.ph1b.audiobook.receiver.LargeWidgetProvider;
import de.ph1b.audiobook.receiver.MediumWidgetProvider;
import de.ph1b.audiobook.utils.L;


public class StateManager {
    private static final String TAG = "StateManager";
    private static StateManager instance;
    private final List<ChangeListener> listeners = new CopyOnWriteArrayList<>(); //to avoid massive synchronization
    private final Context c;
    private PlayerStates state = PlayerStates.STOPPED;
    private int time;
    private boolean sleepTimerActive = false;
    private int position;

    private StateManager(Context c) {
        this.c = c;
    }

    public static synchronized StateManager getInstance(Context c) {
        if (instance == null) {
            instance = new StateManager(c.getApplicationContext());
        }
        return instance;
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
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);

        int largeWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(c, LargeWidgetProvider.class));
        Intent largeIntent = new Intent(c, LargeWidgetProvider.class);
        largeIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        largeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, largeWidgetIds);
        c.sendBroadcast(largeIntent);

        int mediumWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(c, MediumWidgetProvider.class));
        Intent mediumIntent = new Intent(c, MediumWidgetProvider.class);
        mediumIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        mediumIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mediumWidgetIds);
        c.sendBroadcast(mediumIntent);
    }

    public interface ChangeListener {
        public void onTimeChanged(int time);

        public void onStateChanged(PlayerStates state);

        public void onSleepTimerSet(boolean sleepTimerActive);

        public void onPositionChanged(int position);
    }
}

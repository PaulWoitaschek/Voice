package de.ph1b.audiobook.service;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.interfaces.OnTimeChangedListener;
import de.ph1b.audiobook.receiver.WidgetProvider;


public class StateManager {

    private final List<OnStateChangedListener> allStateListener = new ArrayList<>();
    private final List<OnTimeChangedListener> allTimeListener = new ArrayList<>();
    private PlayerStates state = PlayerStates.IDLE;
    private int time = 0;

    private static StateManager instance;
    private final Context c;

    public static synchronized StateManager getInstance(Context c) {
        if (instance == null) {
            instance = new StateManager(c);
        }
        return instance;
    }

    private StateManager(Context c) {
        this.c = c;
    }

    public void addStateChangeListener(OnStateChangedListener listener) {
        allStateListener.add(listener);
    }

    public void removeStateChangeListener(OnStateChangedListener listener) {
        allStateListener.remove(listener);
    }

    public void addTimeChangedListener(OnTimeChangedListener listener) {
        allTimeListener.add(listener);
    }

    public void removeTimeChangedListener(OnTimeChangedListener listener) {
        allTimeListener.remove(listener);
    }

    public PlayerStates getState() {
        return state;
    }

    public void setState(PlayerStates state) {
        this.state = state;
        for (OnStateChangedListener s : allStateListener)
            s.onStateChanged(state);
        updateWidget();
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
        for (OnTimeChangedListener s : allTimeListener)
            s.onTimeChanged(time);
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);
        int ids[] = appWidgetManager.getAppWidgetIds(new ComponentName(c, WidgetProvider.class));
        Intent intent = new Intent(c, WidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        c.sendBroadcast(intent);
    }
}

package de.ph1b.audiobook.service;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.content.MediaDetail;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.interfaces.OnTimeChangedListener;
import de.ph1b.audiobook.receiver.WidgetProvider;


public class StateManager {

    private static StateManager instance;
    private final List<OnStateChangedListener> allStateListener = new ArrayList<>();
    private final List<OnTimeChangedListener> allTimeListener = new ArrayList<>();
    private final Context c;
    public AudioPlayerService.OnSleepStateChangedListener onSleepStateChangedListener;
    private PlayerStates state = PlayerStates.IDLE;
    private MediaDetail media;
    private OnMediaChangedListener onMediaChangedListener;

    private StateManager(Context c) {
        this.c = c;
    }

    public static synchronized StateManager getInstance(Context c) {
        if (instance == null) {
            instance = new StateManager(c);
        }
        return instance;
    }

    public MediaDetail getMedia() {
        return media;
    }

    public void setMedia(MediaDetail media) {
        Log.d("sman", "setting media:" + media.getName());
        this.media = media;
        if (onMediaChangedListener != null) {
            onMediaChangedListener.onMediaChanged(media);
        }
    }

    public void setOnSleepStateChangedListener(AudioPlayerService.OnSleepStateChangedListener onSleepStateChangedListener) {
        this.onSleepStateChangedListener = onSleepStateChangedListener;
    }


    public void addStateChangeListener(OnStateChangedListener listener) {
        allStateListener.add(listener);
    }

    public void setOnMediaChangedListener(OnMediaChangedListener onMediaChangedListener) {
        this.onMediaChangedListener = onMediaChangedListener;
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


    public void setTime(int time) {
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


    public interface OnMediaChangedListener {
        public void onMediaChanged(MediaDetail media);
    }
}

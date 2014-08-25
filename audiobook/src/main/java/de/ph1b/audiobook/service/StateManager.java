package de.ph1b.audiobook.service;


import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.interfaces.OnTimeChangedListener;

public class StateManager {

    private static PlayerStates state = PlayerStates.IDLE;
    private static int time = 0;
    private static final List<OnStateChangedListener> allStateListener = new ArrayList<OnStateChangedListener>();
    private static final List<OnTimeChangedListener> allTimeListener = new ArrayList<OnTimeChangedListener>();


    public static void setStateChangeListener(OnStateChangedListener listener) {
        allStateListener.add(listener);
    }

    public static void setTimeChangedListener(OnTimeChangedListener listener) {
        allTimeListener.add(listener);
    }

    public static void setState(PlayerStates state) {
        StateManager.state = state;
        for (OnStateChangedListener s : allStateListener)
            s.onStateChanged(state);
    }

    public static void setTime(int time) {
        StateManager.time = time;
        for (OnTimeChangedListener s : allTimeListener)
            s.onTimeChanged(time);
    }

    public static PlayerStates getState() {
        return state;
    }

    public static int getTime() {
        return time;
    }
}

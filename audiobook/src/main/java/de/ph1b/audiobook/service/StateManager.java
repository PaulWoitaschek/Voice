package de.ph1b.audiobook.service;

import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.interfaces.OnTimeChangedListener;


public class StateManager {

    private final List<OnStateChangedListener> allStateListener = new ArrayList<>();
    private final List<OnTimeChangedListener> allTimeListener = new ArrayList<>();
    private PlayerStates state = PlayerStates.IDLE;
    private int time = 0;

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
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
        for (OnTimeChangedListener s : allTimeListener)
            s.onTimeChanged(time);
    }
}

package de.ph1b.audiobook.service;


import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.interfaces.OnStateChangedListener;

public class StateManager {

    private static PlayerStates state = PlayerStates.IDLE;
    private static final List<OnStateChangedListener> allListener = new ArrayList<OnStateChangedListener>();

    public static void setStateChangeListener(OnStateChangedListener listener){
        allListener.add(listener);
    }

    public static void setState (PlayerStates state){
        StateManager.state = state;
        for (OnStateChangedListener s : allListener)
            s.onStateChanged(state);
    }

    public static PlayerStates getState() {
        return state;
    }
}

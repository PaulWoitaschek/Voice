package de.ph1b.audiobook.interfaces;

import de.ph1b.audiobook.service.PlayerStates;


public interface OnStateChangedListener {
    public void onStateChanged(PlayerStates state);
}
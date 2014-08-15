package de.ph1b.audiobook.interfaces;

import de.ph1b.audiobook.service.PlayerStates;


public interface OnStateChangedListener {
    void onStateChanged(PlayerStates state);
}
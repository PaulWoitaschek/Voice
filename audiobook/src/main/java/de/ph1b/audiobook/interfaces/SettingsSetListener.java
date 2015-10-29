package de.ph1b.audiobook.interfaces;

/**
 * Interface for indicating that the user settings have changed.
 *
 * @author Paul Woitaschek
 */
public interface SettingsSetListener {
    void onSettingsSet(boolean settingsChanged);
}

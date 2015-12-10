package de.ph1b.audiobook.interfaces

/**
 * Interface for indicating that the user settings have changed.

 * @author Paul Woitaschek
 */
interface SettingsSetListener {
    fun onSettingsSet(settingsChanged: Boolean)
}

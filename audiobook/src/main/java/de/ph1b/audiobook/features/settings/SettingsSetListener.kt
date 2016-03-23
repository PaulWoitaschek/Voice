package de.ph1b.audiobook.features.settings

/**
 * Interface for indicating that the user settings have changed.

 * @author Paul Woitaschek
 */
interface SettingsSetListener {
    fun onSettingsSet(settingsChanged: Boolean)
}

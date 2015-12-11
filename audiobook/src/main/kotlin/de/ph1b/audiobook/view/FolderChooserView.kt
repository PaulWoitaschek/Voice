package de.ph1b.audiobook.view

import java.io.File

/**
 * The view for choosing a root folder.
 *
 * @author Paul Woitaschek
 */
interface FolderChooserView {

    /**
     * @param upEnabled True if the up button should be enabled.
     */
    fun setUpButtonEnabled(upEnabled: Boolean)

    /**
     * @param chooseEnabled True if the choose button should be enabled
     */
    fun setChooseButtonEnabled(chooseEnabled: Boolean)

    /**
     * Sets the new text to display the chosen folder to the user
     *
     * @param text the new text
     */
    fun setCurrentFolderText(text: String)

    /**
     * When navigating to another directory this is called to let the view show the new files
     */
    fun showNewData(newData: List<File>)

    /**
     * When the root folder was changed.
     */
    fun newRootFolders(newFolders: List<File>)

    /**
     * When all tasks are completed this is called and the view should finish.
     *
     * @param chosenFile The file that was selected
     */
    fun finishActivityWithSuccess(chosenFile: File)

    /**
     * Ask the user to add a .nomedia file to the chosen folder so it won't be discovered by music
     * players.
     */
    fun askAddNoMediaFile(folderToHide: File)
}
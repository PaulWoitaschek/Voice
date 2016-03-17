/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.features.folder_chooser

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
     **/
    fun finishWithResult()

    /**
     * Ask the user to add a .nomedia file to the chosen folder so it won't be discovered by music
     * players.
     */
    fun askAddNoMediaFile(folderToHide: File)

    /**
     * The operation mode for view to handle.
     */
    fun getMode(): FolderChooserActivity.OperationMode


    /**
     * Shows a warning that the selected folders are sub-folders of each other.
     */
    fun showSubFolderWarning(first: String, second: String)
}
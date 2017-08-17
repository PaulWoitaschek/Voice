package de.ph1b.audiobook.features.folderChooser

import java.io.File

/**
 * The view for choosing a root folder.
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
  fun finish()

  /**
   * The operation mode for view to handle.
   */
  fun getMode(): FolderChooserActivity.OperationMode

  /**
   * Shows a warning that the selected folders are sub-folders of each other.
   */
  fun showSubFolderWarning(first: String, second: String)
}

package de.ph1b.audiobook.features.folderChooser

import android.annotation.SuppressLint
import android.os.Bundle
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.FileRecognition
import de.ph1b.audiobook.misc.listFilesSafely
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.pref.Pref
import timber.log.Timber
import java.io.File
import java.util.ArrayList
import java.util.HashSet
import javax.inject.Inject
import javax.inject.Named

/**
 * The Presenter for [FolderChooserView]
 */
class FolderChooserPresenter : Presenter<FolderChooserView>() {

  init {
    appComponent.inject(this)
  }

  @field:[Inject Named(PrefKeys.SINGLE_BOOK_FOLDERS)]
  lateinit var singleBookFolderPref: Pref<Set<String>>
  @field:[Inject Named(PrefKeys.COLLECTION_BOOK_FOLDERS)]
  lateinit var collectionBookFolderPref: Pref<Set<String>>
  @Inject
  lateinit var storageDirFinder: StorageDirFinder

  private val rootDirs = ArrayList<File>()
  private val SI_CHOSEN_FILE = "siChosenFile"
  private var chosenFile: File? = null

  override fun onAttach(view: FolderChooserView) {
    refreshRootDirs()
  }

  /** Call this when the read external storage permission was granted. */
  fun gotPermission() {
    refreshRootDirs()
  }

  /**
   * Call this when choose was clicked.
   *
   * Asks the user to add a .nomedia file if there is none. Else calls [FolderChooserView.finish]
   */
  fun chooseClicked() {
    addFileAndTerminate(chosenFile!!)
  }

  /**
   * Returns the closest folder. If this is a folder return itself. Else return the parent.
   */
  private fun File.closestFolder(): File = if (isDirectory) {
    this
  } else {
    parentFile
  }

  /** Call this when a file was selected by the user or the root folder has changed */
  fun fileSelected(selectedFile: File?) {
    chosenFile = selectedFile
    view.apply {
      showNewData(selectedFile?.closestFolder()?.getContentsSorted() ?: emptyList())
      setCurrentFolderText(selectedFile?.name ?: "")
      setUpButtonEnabled(canGoBack())
    }
  }

  private fun canGoBack(): Boolean {
    if (rootDirs.isEmpty()) {
      return false
    }

    // to go up we must not already be in top level
    return rootDirs.none { it == chosenFile!!.closestFolder() }
  }

  /**
   * Call this when the user clicked back.
   *
   * @return true if the presenter handled the back command.
   */
  fun backConsumed(): Boolean {
    Timber.d("up called. currentFolder=$chosenFile")
    return if (canGoBack()) {
      fileSelected(chosenFile!!.closestFolder().parentFile)
      true
    } else {
      false
    }
  }

  private fun addFileAndTerminate(chosen: File) {
    when (view.getMode()) {
      FolderChooserActivity.OperationMode.COLLECTION_BOOK -> {
        if (canAddNewFolder(chosen.absolutePath)) {
          val collections = HashSet(collectionBookFolderPref.value)
          collections.add(chosen.absolutePath)
          collectionBookFolderPref.value = collections
        }
        view.finish()
        Timber.v("chosenCollection = $chosen")
      }
      FolderChooserActivity.OperationMode.SINGLE_BOOK -> {
        if (canAddNewFolder(chosen.absolutePath)) {
          val singleBooks = HashSet(singleBookFolderPref.value)
          singleBooks.add(chosen.absolutePath)
          singleBookFolderPref.value = singleBooks
        }
        view.finish()
        Timber.v("chosenSingleBook = $chosen")
      }
    }
  }

  /**
   * @param newFile the new folder file
   * *
   * @return true if the new folder is not added yet and is no sub- or parent folder of an existing
   * * book folder
   */
  private fun canAddNewFolder(newFile: String): Boolean {
    Timber.v("canAddNewFolder called with $newFile")
    val folders = HashSet(collectionBookFolderPref.value)
    folders.addAll(singleBookFolderPref.value)

    // if this is the first folder adding is always allowed
    if (folders.isEmpty()) {
      return true
    }

    val newParts = newFile.split(File.separator)
    for (s in folders) {

      if (newFile == s) {
        Timber.i("file is already in the list.")
        // same folder, this should not be added
        return false
      }

      val oldParts = s.split(File.separator)
      val max = Math.min(oldParts.size, newParts.size) - 1
      val filesAreSubsets = (0..max).none { oldParts[it] != newParts[it] }
      if (filesAreSubsets) {
        Timber.i("the files are sub folders of each other.")
        view.showSubFolderWarning(s, newFile)
        return false
      }
    }

    return true
  }

  @SuppressLint("MissingPermission")
  private fun refreshRootDirs() {
    rootDirs.clear()
    rootDirs.addAll(storageDirFinder.storageDirs())
    view.newRootFolders(rootDirs)
    view.setChooseButtonEnabled(rootDirs.isNotEmpty())

    when {
      chosenFile != null -> fileSelected(chosenFile)
      rootDirs.isNotEmpty() -> fileSelected(rootDirs.first())
      else -> fileSelected(null)
    }
  }

  /** Gets the containing files of a folder (restricted to music and folders) in a naturally sorted order.  */
  private fun File.getContentsSorted() = listFilesSafely(FileRecognition.folderAndMusicFilter)
    .sortedWith(NaturalOrderComparator.fileComparator)

  override fun onRestore(savedState: Bundle) {
    super.onRestore(savedState)

    chosenFile = savedState.getSerializable(SI_CHOSEN_FILE) as File?
  }

  override fun onSave(state: Bundle) {
    super.onSave(state)

    if (chosenFile != null) {
      state.putSerializable(SI_CHOSEN_FILE, chosenFile!!)
    }
  }

  companion object {
    const val MARSHMALLOW_SD_FALLBACK = "/storage"
  }
}

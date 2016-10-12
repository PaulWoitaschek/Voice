package de.ph1b.audiobook.features.folder_chooser

import android.os.Bundle
import android.os.Environment
import d
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.FileRecognition
import de.ph1b.audiobook.misc.NaturalOrderComparator
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.PrefsManager
import i
import rx.subscriptions.CompositeSubscription
import v
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * The Presenter for [FolderChooserView]
 *
 * @author Paul Woitaschek
 */
class FolderChooserPresenter : Presenter<FolderChooserView>() {

    init {
        App.component().inject(this)
    }

    @Inject lateinit var prefsManager: PrefsManager

    private val rootDirs = ArrayList<File>()
    private val SI_CHOSEN_FILE = "siChosenFile"
    private var chosenFile: File? = null

    override fun onBind(view: FolderChooserView, subscriptions: CompositeSubscription) {
        refreshRootDirs()
    }

    /**
     * Call this when the read external storage permission was granted.
     */
    fun gotPermission() {
        refreshRootDirs()
    }

    /**
     * Call this when choose was clicked.
     *
     * Asks the user to add a .nomedia file if there is none. Else calls [FolderChooserView.finish]
     */
    fun chooseClicked() {
        if (chosenFile!!.isDirectory && !HideFolderDialog.getNoMediaFileByFolder(chosenFile!!).exists()) {
            view!!.askAddNoMediaFile(chosenFile!!)
        } else {
            addFileAndTerminate(chosenFile!!)
        }
    }

    /**
     * Returns the closest folder. If this is a folder return itself. Else return the parent.
     */
    private fun File.closestFolder(): File {
        if (isDirectory) {
            return this
        } else {
            return parentFile
        }
    }


    /**
     * Call this when a file was selected by the user or the root folder has changed
     */
    fun fileSelected(selectedFile: File?) {
        chosenFile = selectedFile
        view!!.apply {
            showNewData(selectedFile?.closestFolder()?.getContentsSorted() ?: emptyList())
            setCurrentFolderText(selectedFile?.name ?: "")
            setUpButtonEnabled(canGoBack())
        }
    }

    private fun canGoBack(): Boolean {
        if (rootDirs.isEmpty()) {
            return false
        }

        for (f in rootDirs) {
            if (f == chosenFile!!.closestFolder()) {
                return false // to go up we must not already be in top level
            }
        }
        return true
    }

    /**
     * Call this when the user clicked back.
     *
     * @return true if the presenter handled the back command.
     */
    fun backConsumed(): Boolean {
        d { "up called. currentFolder=$chosenFile" }
        if (canGoBack()) {
            fileSelected(chosenFile!!.closestFolder().parentFile)
            return true
        } else {
            return false
        }
    }

    /**
     * Call this after the user made a decision on adding a .nomedia file.
     */
    fun hideFolderSelectionMade() {
        addFileAndTerminate(chosenFile!!)
    }

    private fun addFileAndTerminate(chosen: File) {
        when (view!!.getMode()) {
            FolderChooserActivity.OperationMode.COLLECTION_BOOK -> {
                if (canAddNewFolder(chosen.absolutePath)) {
                    val collections = HashSet(prefsManager.collectionFolders.value())
                    collections.add(chosen.absolutePath)
                    prefsManager.collectionFolders.set(collections)
                    view!!.finish()
                }
                v { "chosenCollection = $chosen" }
            }
            FolderChooserActivity.OperationMode.SINGLE_BOOK -> {
                if (canAddNewFolder(chosen.absolutePath)) {
                    val singleBooks = HashSet(prefsManager.singleBookFolders.value())
                    singleBooks.add(chosen.absolutePath)
                    prefsManager.singleBookFolders.set(singleBooks)
                    view!!.finish()
                }
                v { "chosenSingleBook = $chosen" }
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
        v { "canAddNewFolder called with $newFile" }
        val folders = HashSet(prefsManager.collectionFolders.value())
        folders.addAll(prefsManager.singleBookFolders.value())

        // if this is the first folder adding is always allowed
        if (folders.isEmpty()) {
            return true
        }

        val newParts = newFile.split(File.separator)
        for (s in folders) {

            if (newFile == s) {
                i { "file is already in the list." }
                // same folder, this should not be added
                return false
            }

            val oldParts = s.split(File.separator)
            val max = Math.min(oldParts.size, newParts.size) - 1
            var filesAreSubsets = true
            for (i in 0..max) {
                if (oldParts[i] != newParts[i]) {
                    filesAreSubsets = false
                }
            }
            if (filesAreSubsets) {
                i { "the files are sub folders of each other." }
                view!!.showSubFolderWarning(s, newFile)
                return false
            }
        }

        return true
    }

    private fun refreshRootDirs() {
        rootDirs.clear()
        rootDirs.addAll(StorageDirFinder.storageDirs())
        view!!.newRootFolders(rootDirs)
        view!!.setChooseButtonEnabled(rootDirs.isNotEmpty())

        if (chosenFile != null) {
            fileSelected(chosenFile)
        } else if (rootDirs.isNotEmpty()) {
            fileSelected(rootDirs.first())
        } else {
            fileSelected(null)
        }
    }

    /**
     * Gets the containing files of a folder (restricted to music and folders) in a naturally sorted
     * order.
     * *
     * @return The containing files
     */
    private fun File.getContentsSorted(): List<File> {
        val containing = listFiles(FileRecognition.folderAndMusicFilter)//fixme debug this part
        if (containing != null) {
            val asList = ArrayList(Arrays.asList(*containing))
            return asList.sortedWith(NaturalOrderComparator.FILE_COMPARATOR)
        } else {
            return emptyList()
        }
    }

    override fun onRestore(savedState: Bundle?) {
        super.onRestore(savedState)

        chosenFile = savedState?.getSerializable(SI_CHOSEN_FILE) as File?
    }

    override fun onSave(state: Bundle) {
        super.onSave(state)

        if (chosenFile != null) {
            state.putSerializable(SI_CHOSEN_FILE, chosenFile!!)
        }
    }

    companion object {
        val MARSHMALLOW_SD_FALLBACK = "/storage"
    }
}
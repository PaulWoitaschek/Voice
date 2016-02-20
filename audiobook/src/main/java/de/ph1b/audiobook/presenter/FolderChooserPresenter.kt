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

package de.ph1b.audiobook.presenter

import Slimber
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import de.ph1b.audiobook.dialog.HideFolderDialog
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.NaturalOrderComparator
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.utils.FileRecognition
import de.ph1b.audiobook.view.FolderChooserActivity
import de.ph1b.audiobook.view.FolderChooserView
import rx.subscriptions.CompositeSubscription
import java.io.File
import java.util.*
import java.util.regex.Pattern
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
     * Asks the user to add a .nomedia file if there is none. Else calls [FolderChooserView.finishWithResult]
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
        Slimber.d { "up called. currentFolder=$chosenFile" }
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
                    val collections = ArrayList(prefsManager.collectionFolders)
                    collections.add(chosen.absolutePath)
                    prefsManager.collectionFolders = collections
                }
                Slimber.v { "chosenCollection = $chosen" }
            }
            FolderChooserActivity.OperationMode.SINGLE_BOOK -> {
                if (canAddNewFolder(chosen.absolutePath)) {
                    val singleBooks = ArrayList(prefsManager.singleBookFolders)
                    singleBooks.add(chosen.absolutePath)
                    prefsManager.singleBookFolders = singleBooks
                }
                Slimber.v { "chosenSingleBook = $chosen" }
            }
        }

        view!!.finishWithResult()
    }

    /**
     * @param newFile the new folder file
     * *
     * @return true if the new folder is not added yet and is no sub- or parent folder of an existing
     * * book folder
     */
    private fun canAddNewFolder(newFile: String): Boolean {
        Slimber.v { "canAddNewFolder called with $newFile" }
        val folders = ArrayList(prefsManager.collectionFolders)
        folders.addAll(prefsManager.singleBookFolders)

        // if this is the first folder adding is always allowed
        if (folders.isEmpty()) {
            return true
        }

        val newParts = newFile.split(File.separator);
        for (s in folders) {

            if (newFile == s) {
                Slimber.i { "file is already in the list." }
                // same folder, this should not be added
                return false
            }

            val oldParts = s.split(File.separator);
            val max = Math.min(oldParts.size, newParts.size) - 1
            var filesAreSubsets = true;
            for (i in 0..max) {
                if (oldParts[i] != newParts[i]) {
                    filesAreSubsets = false
                }
            }
            if (filesAreSubsets) {
                Slimber.i { "the files are sub folders of each other." }
                view!!.showSubFolderWarning(s, newFile)
                return false
            }
        }

        return true
    }

    private fun refreshRootDirs() {
        rootDirs.clear()
        rootDirs.addAll(storageDirs())
        view!!.newRootFolders(rootDirs)
        view!!.setChooseButtonEnabled(rootDirs.isNotEmpty())

        if (chosenFile != null ) {
            fileSelected(chosenFile)
        } else if (rootDirs.isNotEmpty()) {
            fileSelected(rootDirs.first())
        } else {
            fileSelected(null)
        }
    }


    /**
     * Collects the storage dirs of the device.
     *
     * @return the list of storages.
     */
    private fun storageDirs(): List<File> {
        val dirSeparator = Pattern.compile("/")

        // Final set of paths
        val rv: HashSet<String> = HashSet(5)
        // Primary physical SD-CARD (not emulated)
        val rawExternalStorage = System.getenv("EXTERNAL_STORAGE")
        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        val rawSecondaryStorageStr = System.getenv("SECONDARY_STORAGE")
        // Primary emulated SD-CARD
        val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")

        if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            // Device has physical external storage; use plain paths.
            if (TextUtils.isEmpty(rawExternalStorage)) {
                // EXTERNAL_STORAGE undefined; falling back to default.
                rv.add("/storage/sdcard0")
            } else {
                rv.add(rawExternalStorage)
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            val rawUserId = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ""
            } else {
                val path = Environment.getExternalStorageDirectory().absolutePath
                val folders = dirSeparator.split(path)
                val lastFolder = folders[folders.size - 1]
                var isDigit = false
                try {
                    Integer.valueOf(lastFolder)
                    isDigit = true
                } catch (ignored: NumberFormatException) {
                }
                if (isDigit) lastFolder else ""
            }
            // /storage/emulated/0[1,2,...]
            if (TextUtils.isEmpty(rawUserId)) {
                rv.add(rawEmulatedStorageTarget)
            } else {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId)
            }
        }
        // Add all secondary storage
        if (!TextUtils.isEmpty(rawSecondaryStorageStr)) {
            // All Secondary SD-CARDs splitted into array
            val rawSecondaryStorage = rawSecondaryStorageStr.split(File.pathSeparator)
            rv.addAll(rawSecondaryStorage)
        }
        rv.add("/storage/extSdCard")
        rv.add(Environment.getExternalStorageDirectory().absolutePath)
        rv.add("/storage/emulated/0")
        rv.add("/storage/sdcard1")
        rv.add("/storage/external_SD")
        rv.add("/storage/ext_sd")

        // this is a workaround for marshmallow as we can't know the paths of the sd cards any more.
        // if one of the files in the fallback dir has contents we add it to the list.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fallbackFile = File(MARSHMALLOW_SD_FALLBACK)
            val contents = fallbackFile.listFilesSafely()
            for (content in contents) {
                if (content.listFilesSafely().isNotEmpty()) {
                    rv.add(MARSHMALLOW_SD_FALLBACK)
                    break
                }
            }
        }

        val paths = ArrayList<File>(rv.size)
        for (item  in rv) {
            val f = File(item)
            if (f.listFilesSafely().isNotEmpty()) {
                paths.add(f)
            }
        }
        return paths.sortedWith(NaturalOrderComparator.FILE_COMPARATOR)
    }

    /**
     * As there are cases where [File.listFiles] returns null even though it is a directory, we return
     * an empty list instead.
     */
    private fun File.listFilesSafely(): List<File> {
        val list: Array<File>? = listFiles()
        return list?.toList() ?: emptyList()
    }


    /**
     * Gets the containing files of a folder (restricted to music and folders) in a naturally sorted
     * order.
     * *
     * @return The containing files
     */
    private fun File.getContentsSorted(): List<File> {
        val containing = listFiles(FileRecognition.folderAndMusicFilter)
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
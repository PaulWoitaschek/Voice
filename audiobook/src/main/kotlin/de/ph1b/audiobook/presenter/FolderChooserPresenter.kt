package de.ph1b.audiobook.presenter

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import de.ph1b.audiobook.dialog.HideFolderDialog
import de.ph1b.audiobook.model.NaturalOrderComparator
import de.ph1b.audiobook.utils.FileRecognition
import de.ph1b.audiobook.view.FolderChooserView
import nucleus.presenter.Presenter
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 * The Presenter for [FolderChooserView]
 *
 * @author Paul Woitaschek
 */
class FolderChooserPresenter : Presenter<FolderChooserView>() {

    private val rootDirs = ArrayList<File>()
    private var chosenFile: File? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        chosenFile = savedState?.getSerializable(SI_CHOSEN_FILE) as File?
    }

    override fun onTakeView(view: FolderChooserView) {
        super.onTakeView(view)

        refreshRootDirs()

        if (chosenFile != null ) {
            fileSelected(chosenFile)
        } else if (rootDirs.isNotEmpty()) {
            fileSelected(rootDirs.first())
        } else {
            fileSelected(null)
        }
    }

    override fun onSave(state: Bundle) {
        super.onSave(state)

        state.putSerializable(SI_CHOSEN_FILE, chosenFile)
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
     * Asks the user to add a .nomedia file if there is none. Else calls [FolderChooserView.finishActivityWithSuccess]
     */
    fun chooseClicked() {
        if (chosenFile!!.isDirectory && !HideFolderDialog.getNoMediaFileByFolder(chosenFile!!).exists()) {
            view.askAddNoMediaFile(chosenFile!!)
        } else {
            view.finishActivityWithSuccess(chosenFile!!)
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
        view.showNewData(selectedFile?.closestFolder()?.getContentsSorted() ?: emptyList())
        view.setCurrentFolderText(selectedFile?.name ?: "")
        view.setUpButtonEnabled(canGoBack())
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
        Timber.d("up called. currentFolder=$chosenFile")
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
        view.finishActivityWithSuccess(chosenFile!!)
    }

    private fun refreshRootDirs() {
        rootDirs.clear()
        rootDirs.addAll(storageDirs())
        view.newRootFolders(rootDirs)
        view.setChooseButtonEnabled(rootDirs.isNotEmpty())
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

        val paths = ArrayList<File>(rv.size)
        for (item  in rv) {
            val f = File(item)
            if (f.exists() && f.isDirectory && f.canRead() && f.listFiles() != null && f.listFiles().size > 0) {
                paths.add(f)
            }
        }
        return paths.sortedWith(NaturalOrderComparator.FILE_COMPARATOR)
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

    companion object {
        private val SI_CHOSEN_FILE = "siChosenFile"
    }
}
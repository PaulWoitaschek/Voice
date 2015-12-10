package de.ph1b.audiobook.presenter

import android.app.Activity
import android.content.Intent
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.FolderChooserActivity
import de.ph1b.audiobook.activity.FolderOverviewActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import nucleus.presenter.Presenter
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * The presenter for [FolderOverviewActivity]
 *
 * @author Paul Woitaschek
 */
class FolderOverviewPresenter : Presenter<FolderOverviewActivity> () {

    @Inject internal lateinit var prefsManager: PrefsManager

    init {
        App.component().inject(this);
    }

    override fun onTakeView(view: FolderOverviewActivity) {
        super.onTakeView(view)

        updateFoldersInView()
    }


    /**
     * Handles onActivityResult. Adds the book if it is not in the list yet or sends a message to
     * the user.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == FolderOverviewActivity.PICKER_REQUEST_CODE && data != null) {
            val mode = FolderChooserActivity.OperationMode.valueOf(data.getStringExtra(FolderChooserActivity.RESULT_OPERATION_MODE))
            when (mode) {
                FolderChooserActivity.OperationMode.COLLECTION_BOOK -> {
                    val chosenCollection = data.getStringExtra(FolderChooserActivity.RESULT_CHOSEN_FILE)
                    if (canAddNewFolder(chosenCollection)) {
                        val collections = ArrayList(prefsManager.collectionFolders)
                        collections.add(chosenCollection)
                        prefsManager.collectionFolders = collections
                    }
                    Timber.v("chosenCollection=%s", chosenCollection)
                }
                FolderChooserActivity.OperationMode.SINGLE_BOOK -> {
                    val chosenSingleBook = data.getStringExtra(FolderChooserActivity.RESULT_CHOSEN_FILE)
                    if (canAddNewFolder(chosenSingleBook)) {
                        val singleBooks = ArrayList(prefsManager.singleBookFolders)
                        singleBooks.add(chosenSingleBook)
                        prefsManager.singleBookFolders = singleBooks
                    }
                    Timber.v("chosenSingleBook=%s", chosenSingleBook)
                }
            }
        }
    }

    private fun updateFoldersInView() {
        val collectionFolders = prefsManager.collectionFolders
        val singleFolders = prefsManager.singleBookFolders
        view.updateAdapterData(collectionFolders, singleFolders)
    }

    /**
     * Removes a folder that is either a collection book or a single book.
     *
     * @param folder The folder to remove.
     */
    fun removeFolder(folder: String) {
        val collectionFolders = ArrayList(prefsManager.collectionFolders)
        val singleFolders = ArrayList(prefsManager.singleBookFolders)

        val colRemoved = collectionFolders.remove(folder)
        if (colRemoved) {
            prefsManager.collectionFolders = collectionFolders
        }

        val singleRemoved = singleFolders.remove(folder)
        if (singleRemoved) {
            prefsManager.singleBookFolders = singleFolders
        }

        updateFoldersInView()
    }

    /**
     * @param newFile the new folder file
     * *
     * @return true if the new folder is not added yet and is no sub- or parent folder of an existing
     * * book folder
     */
    private fun canAddNewFolder(newFile: String): Boolean {
        Timber.v("canAddNewFolder called with $newFile")
        val folders = ArrayList(prefsManager.collectionFolders)
        folders.addAll(prefsManager.singleBookFolders)

        // if this is the first folder adding is always allowed
        if (folders.isEmpty()) {
            return true
        }

        val newParts = newFile.split(File.separator);
        for (s in folders) {

            if (newFile == s) {
                Timber.i("file is already in the list.")
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
                Timber.i("the files are sub folders of each other.")
                view.showToast("${view.getString(R.string.adding_failed_subfolder)}\n$s\n$newFile")
                return false
            }
        }

        return true
    }
}
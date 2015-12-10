package de.ph1b.audiobook.presenter

import de.ph1b.audiobook.activity.FolderOverviewActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import nucleus.presenter.RxPresenter
import java.util.*
import javax.inject.Inject

/**
 * The presenter for [FolderOverviewActivity]
 *
 * @author Paul Woitaschek
 */
class FolderOverviewPresenter : RxPresenter<FolderOverviewActivity> () {

    @Inject internal lateinit var prefsManager: PrefsManager

    init {
        App.component().inject(this);
    }

    override fun onTakeView(view: FolderOverviewActivity) {
        super.onTakeView(view)

        view.updateAdapterData(prefsManager.collectionFolders, prefsManager.singleBookFolders)
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

        view.updateAdapterData(collectionFolders, singleFolders)
    }
}
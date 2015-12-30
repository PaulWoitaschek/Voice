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

import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.view.FolderOverviewActivity
import rx.subscriptions.CompositeSubscription
import java.util.*
import javax.inject.Inject

/**
 * The presenter for [FolderOverviewActivity]
 *
 * @author Paul Woitaschek
 */
class FolderOverviewPresenter : Presenter<FolderOverviewActivity>() {

    init {
        App.component().inject(this)
    }

    @Inject lateinit var prefsManager: PrefsManager

    override fun onBind(view: FolderOverviewActivity, subscriptions: CompositeSubscription) {
        updateFoldersInView()
    }

    private fun updateFoldersInView() {
        val collectionFolders = prefsManager.collectionFolders
        val singleFolders = prefsManager.singleBookFolders
        view!!.updateAdapterData(collectionFolders, singleFolders)
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
}
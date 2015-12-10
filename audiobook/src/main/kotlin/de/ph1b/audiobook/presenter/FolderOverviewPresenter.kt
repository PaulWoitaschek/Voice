package de.ph1b.audiobook.presenter

import de.ph1b.audiobook.activity.FolderOverviewActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import nucleus.presenter.RxPresenter
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
}
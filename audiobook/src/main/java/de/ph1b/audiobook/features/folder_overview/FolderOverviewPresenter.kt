package de.ph1b.audiobook.features.folder_overview

import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.PrefsManager
import rx.Observable
import rx.subscriptions.CompositeSubscription
import java.util.*
import javax.inject.Inject

/**
 * The presenter for [FolderOverviewController]
 *
 * @author Paul Woitaschek
 */
class FolderOverviewPresenter : Presenter<FolderOverviewController>() {

    init {
        App.component().inject(this)
    }

    @Inject lateinit var prefsManager: PrefsManager

    override fun onBind(view: FolderOverviewController, subscriptions: CompositeSubscription) {

        val collectionFolderStream = prefsManager.collectionFolders.asObservable()
                .map { it.map { FolderModel(it, true) } }
        val singleFolderStream = prefsManager.singleBookFolders.asObservable()
                .map { it.map { FolderModel(it, false) } }

        subscriptions.add(Observable.combineLatest(collectionFolderStream, singleFolderStream, { collection, single -> collection + single })
                .subscribe { view.newData(it) })
    }


    /** removes a selected folder **/
    fun removeFolder(folder: FolderModel) {
        prefsManager.collectionFolders.asObservable()
                .map { HashSet(it) }
                .first()
                .subscribe {
                    val removed = it.remove(folder.folder)
                    if (removed) prefsManager.collectionFolders.set(it)
                }

        prefsManager.singleBookFolders.asObservable()
                .map { HashSet(it) }
                .first()
                .subscribe {
                    val removed = it.remove(folder.folder)
                    if (removed) prefsManager.singleBookFolders.set(it)
                }
    }
}
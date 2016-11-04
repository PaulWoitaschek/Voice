package de.ph1b.audiobook.features.folder_overview

import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.PrefsManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
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

    override fun onBind(view: FolderOverviewController, disposables: CompositeDisposable) {

        val collectionFolderStream = prefsManager.collectionFolders.asV2Observable()
                .map { it.map { FolderModel(it, true) } }
        val singleFolderStream = prefsManager.singleBookFolders.asV2Observable()
                .map { it.map { FolderModel(it, false) } }

        val combined = Observable.combineLatest(collectionFolderStream, singleFolderStream, BiFunction<List<FolderModel>, List<FolderModel>, List<FolderModel>> { t1, t2 -> t1 + t2 })
        disposables.add(combined
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
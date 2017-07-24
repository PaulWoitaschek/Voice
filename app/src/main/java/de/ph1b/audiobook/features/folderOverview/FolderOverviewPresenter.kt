package de.ph1b.audiobook.features.folderOverview

import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.combineLatest
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.PrefsManager
import io.reactivex.disposables.CompositeDisposable
import java.util.HashSet
import javax.inject.Inject

/**
 * The presenter for [FolderOverviewController]
 */
class FolderOverviewPresenter : Presenter<FolderOverviewController>() {

  init {
    App.component.inject(this)
  }

  @Inject lateinit var prefsManager: PrefsManager

  override fun onBind(view: FolderOverviewController, disposables: CompositeDisposable) {
    val collectionFolderStream = prefsManager.collectionFolders.asV2Observable()
        .map { it.map { FolderModel(it, true) } }
    val singleFolderStream = prefsManager.singleBookFolders.asV2Observable()
        .map { it.map { FolderModel(it, false) } }

    val combined = combineLatest(collectionFolderStream, singleFolderStream) { t1, t2 -> t1 + t2 }
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
          if (removed) prefsManager.collectionFolders.value = it
        }

    prefsManager.singleBookFolders.asObservable()
        .map { HashSet(it) }
        .first()
        .subscribe {
          val removed = it.remove(folder.folder)
          if (removed) prefsManager.singleBookFolders.value = it
        }
  }
}

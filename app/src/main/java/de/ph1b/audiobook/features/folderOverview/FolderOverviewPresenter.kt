package de.ph1b.audiobook.features.folderOverview

import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.Observables
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.pref.Pref
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * The presenter for [FolderOverviewController]
 */
class FolderOverviewPresenter : Presenter<FolderOverviewController>() {

  init {
    App.component.inject(this)
  }

  @field:[Inject Named(PrefKeys.SINGLE_BOOK_FOLDERS)]
  lateinit var singleBookFolderPref: Pref<Set<String>>
  @field:[Inject Named(PrefKeys.COLLECTION_BOOK_FOLDERS)]
  lateinit var collectionBookFolderPref: Pref<Set<String>>

  override fun onAttach(view: FolderOverviewController) {
    val collectionFolderStream = collectionBookFolderPref.stream
      .map { it.map { FolderModel(it, true) } }
    val singleFolderStream = singleBookFolderPref.stream
      .map { it.map { FolderModel(it, false) } }

    Observables.combineLatest(collectionFolderStream, singleFolderStream) { t1, t2 -> t1 + t2 }
      .subscribe { view.newData(it) }
      .disposeOnDetach()
  }

  /** removes a selected folder **/
  fun removeFolder(folder: FolderModel) {
    collectionBookFolderPref.stream
      .map { HashSet(it) }
      .firstOrError()
      .subscribe { it ->
        val removed = it.remove(folder.folder)
        if (removed) collectionBookFolderPref.value = it
      }

    singleBookFolderPref.stream
      .map { HashSet(it) }
      .firstOrError()
      .subscribe { it ->
        val removed = it.remove(folder.folder)
        if (removed) singleBookFolderPref.value = it
      }
  }
}

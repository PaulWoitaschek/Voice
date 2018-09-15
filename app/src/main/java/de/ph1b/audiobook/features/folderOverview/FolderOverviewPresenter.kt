package de.ph1b.audiobook.features.folderOverview

import android.annotation.SuppressLint
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.Observables
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.pref.Pref
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.HashSet

/**
 * The presenter for [FolderOverviewController]
 */
class FolderOverviewPresenter : Presenter<FolderOverviewController>(), KoinComponent {

  private val singleBookFolderPref: Pref<Set<String>> by inject(PrefKeys.SINGLE_BOOK_FOLDERS)
  private val collectionBookFolderPref: Pref<Set<String>> by inject(PrefKeys.COLLECTION_BOOK_FOLDERS)

  override fun onAttach(view: FolderOverviewController) {
    val collectionFolderStream = collectionBookFolderPref.stream
      .map { folders -> folders.map { FolderModel(it, true) } }
    val singleFolderStream = singleBookFolderPref.stream
      .map { folders -> folders.map { FolderModel(it, false) } }

    Observables.combineLatest(collectionFolderStream, singleFolderStream) { t1, t2 -> t1 + t2 }
      .subscribe { view.newData(it) }
      .disposeOnDetach()
  }

  @SuppressLint("CheckResult")
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

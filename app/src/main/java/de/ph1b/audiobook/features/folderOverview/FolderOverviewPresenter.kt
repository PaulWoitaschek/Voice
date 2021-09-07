package de.ph1b.audiobook.features.folderOverview

import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.features.folderOverview.FolderModel.Companion.FOLDER_COLLECTION
import de.ph1b.audiobook.features.folderOverview.FolderModel.Companion.FOLDER_NO_COLLECTION
import de.ph1b.audiobook.features.folderOverview.FolderModel.Companion.FOLDER_RECURSIVE
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.mvp.Presenter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * The presenter for [FolderOverviewController]
 */
class FolderOverviewPresenter : Presenter<FolderOverviewController>() {

  init {
    appComponent.inject(this)
  }

  @field:[Inject Named(PrefKeys.SINGLE_BOOK_FOLDERS)]
  lateinit var singleBookFolderPref: Pref<Set<String>>
  @field:[Inject Named(PrefKeys.COLLECTION_BOOK_FOLDERS)]
  lateinit var collectionBookFolderPref: Pref<Set<String>>
  @field:[Inject Named(PrefKeys.RECURSIVE_BOOK_FOLDERS)]
  lateinit var recursiveBookFolderPref: Pref<Set<String>>

  override fun onAttach(view: FolderOverviewController) {
    val collectionFolderStream = collectionBookFolderPref.flow
      .map { set -> set.map { FolderModel(it, FOLDER_COLLECTION) } }
    val singleFolderStream = singleBookFolderPref.flow
      .map { set -> set.map { FolderModel(it, FOLDER_NO_COLLECTION) } }
    val recursiveFolderStream = recursiveBookFolderPref.flow
      .map { set -> set.map { FolderModel(it, FOLDER_RECURSIVE) } }

    onAttachScope.launch {
      combine(collectionFolderStream, singleFolderStream, recursiveFolderStream) { t1, t2, t3 -> t1 + t2 + t3}
        .collect { view.newData(it) }
    }
  }

  /** removes a selected folder **/
  fun removeFolder(folder: FolderModel) {
    scope.launch {
      val folders = collectionBookFolderPref.flow.first().toMutableSet()
      val removed = folders.remove(folder.folder)
      if (removed) collectionBookFolderPref.value = folders
    }

    scope.launch {
      val folders = singleBookFolderPref.flow.first().toMutableSet()
      val removed = folders.remove(folder.folder)
      if (removed) singleBookFolderPref.value = folders
    }

    scope.launch {
      val folders = recursiveBookFolderPref.flow.first().toMutableSet()
      val removed = folders.remove(folder.folder)
      if (removed) recursiveBookFolderPref.value = folders
    }
  }
}

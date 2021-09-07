package de.ph1b.audiobook.features.folderOverview

import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import java.io.File

data class FolderModel(val folder: String, val type: Int) : Comparable<FolderModel> {

  companion object {
    const val FOLDER_NO_COLLECTION = 0
    const val FOLDER_COLLECTION = 1
    const val FOLDER_RECURSIVE = 2
  }

  override fun compareTo(other: FolderModel): Int {
    return NaturalOrderComparator.fileComparator.compare(File(folder), File(other.folder))
  }
}

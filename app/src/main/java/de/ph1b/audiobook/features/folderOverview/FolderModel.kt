package de.ph1b.audiobook.features.folderOverview

import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.features.folderChooser.FolderChooserActivity
import java.io.File

data class FolderModel(val folder: String, val isCollection: FolderChooserActivity.OperationMode) : Comparable<FolderModel> {
  override fun compareTo(other: FolderModel): Int {
    val isCollectionCompare = other.isCollection.compareTo(isCollection)
    if (isCollectionCompare != 0) return isCollectionCompare

    return NaturalOrderComparator.fileComparator.compare(File(folder), File(other.folder))
  }
}

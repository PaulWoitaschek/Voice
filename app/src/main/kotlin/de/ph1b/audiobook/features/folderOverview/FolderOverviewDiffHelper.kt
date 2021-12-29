package de.ph1b.audiobook.features.folderOverview

import androidx.recyclerview.widget.DiffUtil

/**
 * Calculate a diff between the two lists
 */
object FolderOverviewDiffHelper {

  fun diff(old: List<FolderModel>, new: List<FolderModel>): DiffUtil.DiffResult {
    return DiffUtil.calculateDiff(
      object : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
          old[oldItemPosition] == new[newItemPosition]

        override fun getOldListSize(): Int = old.size

        override fun getNewListSize(): Int = new.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
          areItemsTheSame(
            oldItemPosition,
            newItemPosition
          )
      },
      false
    ) // assume sorted
  }
}

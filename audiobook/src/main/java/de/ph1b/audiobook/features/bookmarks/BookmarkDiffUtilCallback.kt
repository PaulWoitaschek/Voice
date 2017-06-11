package de.ph1b.audiobook.features.bookmarks

import android.support.v7.util.DiffUtil
import de.ph1b.audiobook.Bookmark

/**
 * Calculates the diff between two bookmark lists.
 *
 * @author Paul Woitaschek
 */
class BookmarkDiffUtilCallback(
    private val oldItems: List<Bookmark>,
    private val newItems: List<Bookmark>
) : DiffUtil.Callback() {

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldItems[oldItemPosition] == newItems[newItemPosition]
  }

  override fun getOldListSize() = oldItems.size

  override fun getNewListSize() = newItems.size

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = areItemsTheSame(oldItemPosition, newItemPosition)
}

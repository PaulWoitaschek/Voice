package voice.app.features.bookmarks.list

import androidx.recyclerview.widget.DiffUtil
import voice.data.Bookmark

/**
 * Calculates the diff between two bookmark lists.
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

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
    areItemsTheSame(oldItemPosition, newItemPosition)
}

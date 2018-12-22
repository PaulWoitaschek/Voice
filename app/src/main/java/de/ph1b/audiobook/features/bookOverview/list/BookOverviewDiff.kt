package de.ph1b.audiobook.features.bookOverview.list

import androidx.recyclerview.widget.DiffUtil
import de.ph1b.audiobook.data.Book

class BookOverviewDiff : DiffUtil.ItemCallback<Any>() {

  override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
    return if (oldItem is Book && newItem is Book) {
      (oldItem.id == newItem.id &&
          oldItem.content.position == newItem.content.position &&
          oldItem.name == newItem.name)
    } else oldItem == newItem
  }

  override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
    return if (oldItem is Book && newItem is Book) {
      oldItem.id == newItem.id
    } else {
      oldItem == newItem
    }
  }
}

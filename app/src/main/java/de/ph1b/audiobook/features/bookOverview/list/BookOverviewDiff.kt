package de.ph1b.audiobook.features.bookOverview.list

import androidx.recyclerview.widget.DiffUtil

class BookOverviewDiff : DiffUtil.ItemCallback<Any>() {

  override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
    return if (oldItem is BookOverviewModel && newItem is BookOverviewModel) {
      oldItem.areContentsTheSame(newItem)
    } else oldItem == newItem
  }

  override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
    return if (oldItem is BookOverviewModel && newItem is BookOverviewModel) {
      oldItem.areItemsTheSame(newItem)
    } else {
      oldItem == newItem
    }
  }
}

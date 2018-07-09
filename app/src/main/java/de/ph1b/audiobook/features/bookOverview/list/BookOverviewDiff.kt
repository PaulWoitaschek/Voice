package de.ph1b.audiobook.features.bookOverview.list

import android.support.v7.util.DiffUtil
import de.ph1b.audiobook.data.Book

class BookOverviewDiff : DiffUtil.ItemCallback<Book>() {

  override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
    return oldItem.id == newItem.id
        && oldItem.content.position == newItem.content.position
        && oldItem.name == newItem.name
  }

  override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
    return oldItem.id == newItem.id
  }
}

package de.ph1b.audiobook.features.bookOverview.list

import android.support.v7.util.DiffUtil
import de.ph1b.audiobook.data.Book

class BookShelfDiffCallback(
    private val oldBooks: List<Book>,
    private val newBooks: List<Book>
) : DiffUtil.Callback() {

  override fun getOldListSize(): Int = oldBooks.size

  override fun getNewListSize(): Int = newBooks.size

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    val oldItem = oldBooks[oldItemPosition]
    val newItem = newBooks[newItemPosition]
    return oldItem.id == newItem.id && oldItem.position == newItem.position && oldItem.name == newItem.name
  }

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    val oldItem = oldBooks[oldItemPosition]
    val newItem = newBooks[newItemPosition]
    return oldItem.id == newItem.id
  }
}

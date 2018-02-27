package de.ph1b.audiobook.features.bookOverview.list

import android.support.v7.recyclerview.extensions.ListAdapter
import android.view.ViewGroup
import de.ph1b.audiobook.data.Book

class BookShelfAdapter(
  private val bookClicked: (Book, BookShelfClick) -> Unit
) : ListAdapter<Book, BookShelfHolder>(BookShelfDiffCallback()) {

  init {
    setHasStableIds(true)
  }

  fun reloadBookCover(bookId: Long) {
    for (i in 0 until itemCount) {
      if (getItem(i).id == bookId) {
        notifyItemChanged(i)
        break
      }
    }
  }

  override fun getItemId(position: Int) = getItem(position).id

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    BookShelfHolder(parent, bookClicked)

  override fun onBindViewHolder(holder: BookShelfHolder, position: Int) {
    holder.bind(getItem(position))
  }
}

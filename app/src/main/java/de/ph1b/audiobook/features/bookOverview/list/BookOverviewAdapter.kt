package de.ph1b.audiobook.features.bookOverview.list

import androidx.recyclerview.widget.RecyclerView
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewHeaderComponent
import de.ph1b.audiobook.misc.recyclerComponent.CompositeListAdapter
import java.util.UUID

typealias BookClickListener = (Book, BookOverviewClick) -> Unit

class BookOverviewAdapter(
  bookClickListener: BookClickListener
) : CompositeListAdapter<Any>(BookOverviewDiff()) {

  init {
    addComponent(BookOverviewComponent(bookClickListener))
    addComponent(BookOverviewHeaderComponent())
  }

  fun reloadBookCover(bookId: UUID) {
    for (i in 0 until itemCount) {
      val item = getItem(i)
      if (item is Book && item.id == bookId) {
        notifyItemChanged(i)
        break
      }
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    super.onBindViewHolder(holder, position)
    val item = getItem(position)
    if (item is Book) {
      holder as BookOverviewHolder
      holder.setPlaying(item.id == activeBookId)
    }
  }

  private var activeBookId: UUID? = null

  fun setActiveBook(id: UUID?) {
    if (activeBookId != id) {
      for (position in 0 until itemCount) {
        val item = getItem(position)
        if (item is Book) {
          if (item.id == id || item.id == activeBookId) {
            notifyItemChanged(position)
          }
        }
      }
      activeBookId = id
    }
  }
}

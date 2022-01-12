package de.ph1b.audiobook.features.bookCategory

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil
import de.ph1b.audiobook.features.bookOverview.list.BookClickListener
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewViewState
import de.ph1b.audiobook.features.bookOverview.list.GridBookOverviewComponent
import de.ph1b.audiobook.features.bookOverview.list.ListBookOverviewComponent
import de.ph1b.audiobook.misc.recyclerComponent.CompositeListAdapter

class BookCategoryAdapter(listener: BookClickListener) :
  CompositeListAdapter<BookOverviewViewState>(Diff()) {

  init {
    addComponent(GridBookOverviewComponent(listener))
    addComponent(ListBookOverviewComponent(listener))
  }

  fun notifyCoverChanged(bookId: Uri) {
    for (i in 0 until itemCount) {
      val item = getItem(i)
      if (item.id == bookId) {
        notifyItemChanged(i)
        return
      }
    }
  }
}

private class Diff : DiffUtil.ItemCallback<BookOverviewViewState>() {

  override fun areItemsTheSame(oldItem: BookOverviewViewState, newItem: BookOverviewViewState): Boolean {
    return oldItem.areItemsTheSame(newItem)
  }

  override fun areContentsTheSame(oldItem: BookOverviewViewState, newItem: BookOverviewViewState): Boolean {
    return oldItem.areContentsTheSame(newItem)
  }
}

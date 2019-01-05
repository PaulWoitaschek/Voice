package de.ph1b.audiobook.features.bookCategory

import androidx.recyclerview.widget.DiffUtil
import de.ph1b.audiobook.features.bookOverview.list.BookClickListener
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewModel
import de.ph1b.audiobook.features.bookOverview.list.GridBookOverviewComponent
import de.ph1b.audiobook.features.bookOverview.list.ListBookOverviewComponent
import de.ph1b.audiobook.misc.recyclerComponent.CompositeListAdapter

class BookCategoryAdapter(listener: BookClickListener) :
  CompositeListAdapter<BookOverviewModel>(Diff()) {

  init {
    addComponent(GridBookOverviewComponent(listener))
    addComponent(ListBookOverviewComponent(listener))
  }
}

private class Diff : DiffUtil.ItemCallback<BookOverviewModel>() {

  override fun areItemsTheSame(oldItem: BookOverviewModel, newItem: BookOverviewModel): Boolean {
    return oldItem.areItemsTheSame(newItem)
  }

  override fun areContentsTheSame(oldItem: BookOverviewModel, newItem: BookOverviewModel): Boolean {
    return oldItem.areContentsTheSame(newItem)
  }
}

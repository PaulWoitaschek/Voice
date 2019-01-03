package de.ph1b.audiobook.features.bookCategory

import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import de.ph1b.audiobook.features.bookOverview.list.BookClickListener
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewComponent
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewModel
import de.ph1b.audiobook.misc.recyclerComponent.CompositeListAdapter

class BookCategoryAdapter(context: Context, listener: BookClickListener) :
  CompositeListAdapter<BookOverviewModel>(Diff()) {

  init {
    addComponent(BookOverviewComponent(listener, context))
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

package de.ph1b.audiobook.features.bookOverview.list

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewHeaderComponent
import de.ph1b.audiobook.misc.recyclerComponent.CompositeListAdapter

typealias BookClickListener = (Book.Id, BookOverviewClick) -> Unit

class BookOverviewAdapter(
  bookClickListener: BookClickListener,
) : CompositeListAdapter<BookOverviewItem>(BookOverviewDiff()) {

  init {
    addComponent(GridBookOverviewComponent(bookClickListener))
    addComponent(ListBookOverviewComponent(bookClickListener))
    addComponent(BookOverviewHeaderComponent())
  }

  fun itemAtPositionIsHeader(position: Int): Boolean {
    val item = getItem(position)
    return item is BookOverviewHeaderModel
  }
}

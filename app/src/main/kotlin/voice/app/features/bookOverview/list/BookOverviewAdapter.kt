package voice.app.features.bookOverview.list

import voice.app.features.bookOverview.list.header.BookOverviewHeaderComponent
import voice.app.misc.recyclerComponent.CompositeListAdapter
import voice.bookOverview.BookOverviewHeaderModel
import voice.bookOverview.BookOverviewItem
import voice.data.Book

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

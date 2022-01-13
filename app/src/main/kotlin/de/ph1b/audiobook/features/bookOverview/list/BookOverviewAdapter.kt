package de.ph1b.audiobook.features.bookOverview.list

import android.net.Uri
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewHeaderComponent
import de.ph1b.audiobook.features.bookOverview.list.header.OpenCategoryListener
import de.ph1b.audiobook.misc.recyclerComponent.CompositeListAdapter

typealias BookClickListener = (Uri, BookOverviewClick) -> Unit

class BookOverviewAdapter(
  bookClickListener: BookClickListener,
  openCategoryListener: OpenCategoryListener
) : CompositeListAdapter<BookOverviewItem>(BookOverviewDiff()) {

  init {
    addComponent(GridBookOverviewComponent(bookClickListener))
    addComponent(ListBookOverviewComponent(bookClickListener))
    addComponent(BookOverviewHeaderComponent(openCategoryListener))
  }

  fun reloadBookCover(bookId: Uri) {
    for (i in 0 until itemCount) {
      val item = getItem(i)
      if (item is BookOverviewViewState && item.id == bookId) {
        notifyItemChanged(i)
        break
      }
    }
  }

  fun itemAtPositionIsHeader(position: Int): Boolean {
    val item = getItem(position)
    return item is BookOverviewHeaderModel
  }
}

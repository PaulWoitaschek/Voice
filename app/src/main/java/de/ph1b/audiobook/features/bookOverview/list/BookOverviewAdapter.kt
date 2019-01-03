package de.ph1b.audiobook.features.bookOverview.list

import android.content.Context
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewHeaderComponent
import de.ph1b.audiobook.features.bookOverview.list.header.OpenCategoryListener
import de.ph1b.audiobook.misc.recyclerComponent.CompositeListAdapter
import java.util.UUID

typealias BookClickListener = (Book, BookOverviewClick) -> Unit

class BookOverviewAdapter(
  bookClickListener: BookClickListener,
  openCategoryListener: OpenCategoryListener,
  context: Context
) : CompositeListAdapter<BookOverviewItem>(BookOverviewDiff()) {

  init {
    addComponent(BookOverviewComponent(bookClickListener, context))
    addComponent(BookOverviewHeaderComponent(openCategoryListener))
  }

  fun reloadBookCover(bookId: UUID) {
    for (i in 0 until itemCount) {
      val item = getItem(i)
      if (item is BookOverviewModel && item.book.id == bookId) {
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

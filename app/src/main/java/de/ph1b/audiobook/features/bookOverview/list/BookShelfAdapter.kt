package de.ph1b.audiobook.features.bookOverview.list

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import de.ph1b.audiobook.data.Book
import java.util.ArrayList

class BookShelfAdapter(
    private val bookClicked: (Book, BookShelfClick) -> Unit
) : RecyclerView.Adapter<BookShelfHolder>() {

  private val books = ArrayList<Book>()

  init {
    setHasStableIds(true)
  }

  fun newDataSet(newBooks: List<Book>) {
    val oldBooks = books.toList()
    books.clear()
    books.addAll(newBooks)
    val callback = BookShelfDiffCallback(oldBooks = oldBooks, newBooks = books)
    val diffResult = DiffUtil.calculateDiff(callback, false)
    diffResult.dispatchUpdatesTo(this)
  }

  fun reloadBookCover(bookId: Long) {
    val index = books.indexOfFirst { it.id == bookId }
    if (index >= 0) {
      notifyItemChanged(index)
    }
  }

  override fun getItemId(position: Int) = books[position].id

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BookShelfHolder(parent, bookClicked)

  override fun onBindViewHolder(holder: BookShelfHolder, position: Int) = holder.bind(books[position])

  override fun getItemCount(): Int = books.size

}

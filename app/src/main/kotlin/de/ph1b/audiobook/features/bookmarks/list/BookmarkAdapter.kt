package de.ph1b.audiobook.features.bookmarks.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import de.ph1b.audiobook.data.Bookmark2
import de.ph1b.audiobook.data.Chapter2

/**
 * Adapter for displaying a list of bookmarks.
 */
class BookmarkAdapter(
  private val listener: BookmarkClickListener
) : RecyclerView.Adapter<BookMarkHolder>() {

  private val bookmarks = ArrayList<Bookmark2>()
  private val chapters = ArrayList<Chapter2>()

  fun newData(bookmarks: List<Bookmark2>, chapters: List<Chapter2>) {
    this.chapters.clear()
    this.chapters.addAll(chapters)

    val newBookmarks = bookmarks
    val callback = BookmarkDiffUtilCallback(this.bookmarks, newBookmarks)
    val diff = DiffUtil.calculateDiff(callback)
    this.bookmarks.clear()
    this.bookmarks.addAll(newBookmarks)
    diff.dispatchUpdatesTo(this)
  }

  fun indexOf(bookmark: Bookmark2) = bookmarks.indexOf(bookmark)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    BookMarkHolder(parent, listener)

  override fun onBindViewHolder(holder: BookMarkHolder, position: Int) {
    val bookMark = bookmarks[position]
    holder.bind(bookMark, chapters)
  }

  override fun getItemCount(): Int = bookmarks.size
}

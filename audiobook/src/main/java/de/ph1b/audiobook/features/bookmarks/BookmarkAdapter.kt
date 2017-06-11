package de.ph1b.audiobook.features.bookmarks

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.Chapter
import java.util.*

/**
 * Adapter for displaying a list of bookmarks.
 *
 * @author Paul Woitaschek
 */
class BookmarkAdapter(
    private val chapters: List<Chapter>,
    private val listener: BookmarkClickListener
) : RecyclerView.Adapter<BookMarkHolder>() {

  private val bookmarks = ArrayList<Bookmark>()

  fun newData(bookmarks: List<Bookmark>) {
    val newBookmarks = bookmarks.sorted()
    val callback = BookmarkDiffUtilCallback(this.bookmarks, newBookmarks)
    val diff = DiffUtil.calculateDiff(callback)
    this.bookmarks.clear()
    this.bookmarks.addAll(newBookmarks)
    diff.dispatchUpdatesTo(this)
  }

  fun indexOf(bookmark: Bookmark) = bookmarks.indexOf(bookmark)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BookMarkHolder(parent, listener)

  override fun onBindViewHolder(holder: BookMarkHolder, position: Int) {
    val bookMark = bookmarks[position]
    holder.bind(bookMark, chapters)
  }

  override fun getItemCount(): Int = bookmarks.size
}

package de.ph1b.audiobook.features.bookmarks.list

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Chapter

/**
 * Adapter for displaying a list of bookmarks.
 */
class BookmarkAdapter(
  private val listener: BookmarkClickListener
) : RecyclerView.Adapter<BookMarkHolder>() {

  private val bookmarks = ArrayList<Bookmark>()
  private val chapters = ArrayList<Chapter>()

  fun newData(bookmarks: List<Bookmark>, chapters: List<Chapter>) {
    this.chapters.clear()
    this.chapters.addAll(chapters)

    val newBookmarks = bookmarks.sorted().reversed()
    val callback = BookmarkDiffUtilCallback(this.bookmarks, newBookmarks)
    val diff = DiffUtil.calculateDiff(callback)
    this.bookmarks.clear()
    this.bookmarks.addAll(newBookmarks)
    diff.dispatchUpdatesTo(this)
  }

  fun indexOf(bookmark: Bookmark) = bookmarks.indexOf(bookmark)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    BookMarkHolder(parent, listener)

  override fun onBindViewHolder(holder: BookMarkHolder, position: Int) {
    val bookMark = bookmarks[position]
    holder.bind(bookMark, chapters)
  }

  override fun getItemCount(): Int = bookmarks.size
}

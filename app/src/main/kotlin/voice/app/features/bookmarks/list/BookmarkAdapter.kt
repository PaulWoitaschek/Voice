package voice.app.features.bookmarks.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import voice.data.Bookmark
import voice.data.Chapter

/**
 * Adapter for displaying a list of bookmarks.
 */
class BookmarkAdapter(
  private val listener: BookmarkClickListener,
) : RecyclerView.Adapter<BookMarkHolder>() {

  private val bookmarks = ArrayList<Bookmark>()
  private val chapters = ArrayList<Chapter>()

  fun newData(
    bookmarks: List<Bookmark>,
    chapters: List<Chapter>,
  ) {
    this.chapters.clear()
    this.chapters.addAll(chapters)

    val newBookmarks = bookmarks
    val callback = BookmarkDiffUtilCallback(this.bookmarks, newBookmarks)
    val diff = DiffUtil.calculateDiff(callback)
    this.bookmarks.clear()
    this.bookmarks.addAll(newBookmarks)
    diff.dispatchUpdatesTo(this)
  }

  fun indexOf(bookmark: Bookmark) = bookmarks.indexOf(bookmark)

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = BookMarkHolder(parent, listener)

  override fun onBindViewHolder(
    holder: BookMarkHolder,
    position: Int,
  ) {
    val bookMark = bookmarks[position]
    holder.bind(bookMark, chapters)
  }

  override fun getItemCount(): Int = bookmarks.size
}

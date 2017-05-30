package de.ph1b.audiobook.features.bookmarks

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.misc.layoutInflater
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying a list of bookmarks.
 *
 * @author Paul Woitaschek
 */
class BookmarkAdapter(private val chapters: List<Chapter>, private val listener: OnOptionsMenuClickedListener, private val context: Context) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

  private val bookmarks = ArrayList<Bookmark>()

  private fun formatTime(ms: Int): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms.toLong()).toString()
    val m = "%02d".format((TimeUnit.MILLISECONDS.toMinutes(ms.toLong()) % 60))
    val s = "%02d".format((TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60))
    var returnString = ""
    if (h != "0") {
      returnString += h + ":"
    }
    returnString += m + ":" + s
    return returnString
  }

  fun remove(bookmark: Bookmark) {
    val index = bookmarks.indexOf(bookmark)
    bookmarks.remove(bookmark)
    notifyItemRemoved(index)
  }

  fun add(bookmark: Bookmark) {
    bookmarks.add(bookmark)
    bookmarks.sort()
    val index = bookmarks.indexOf(bookmark)
    notifyItemInserted(index)
  }

  fun addAll(bookmarks: Iterable<Bookmark>) {
    this.bookmarks.addAll(bookmarks)
    this.bookmarks.sort()
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val v = parent.layoutInflater().inflate(R.layout.dialog_bookmark_row_layout, parent, false)
    return ViewHolder(v, listener)
  }

  fun replace(oldBookmark: Bookmark, newBookmark: Bookmark) {
    val oldIndex = bookmarks.indexOf(oldBookmark)
    bookmarks[oldIndex] = newBookmark
    notifyItemChanged(oldIndex)
    bookmarks.sort()
    notifyItemMoved(oldIndex, bookmarks.indexOf(newBookmark))
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(position)
  }

  override fun getItemCount(): Int = bookmarks.size

  interface OnOptionsMenuClickedListener {

    fun onOptionsMenuClicked(bookmark: Bookmark, v: View)

    fun onBookmarkClicked(bookmark: Bookmark)
  }

  inner class ViewHolder(itemView: View, listener: OnOptionsMenuClickedListener) : RecyclerView.ViewHolder(itemView) {

    private val title: TextView = find(R.id.title)
    private val summary: TextView = find(R.id.summary)
    private val time: TextView = find(R.id.time)
    private val edit: View = find(R.id.edit)

    fun bind(position: Int) {
      val bookmark = bookmarks[position]
      title.text = bookmark.title

      val size = chapters.size
      val currentChapter = chapters.single { it.file == bookmark.mediaFile }
      val index = chapters.indexOf(currentChapter)

      summary.text = context.getString(R.string.format_bookmarks_n_of, index + 1, size)
      time.text = context.getString(R.string.format_bookmarks_time, formatTime(bookmark.time),
          formatTime(currentChapter.duration))
    }

    init {
      edit.setOnClickListener { listener.onOptionsMenuClicked(bookmarks[adapterPosition], it) }
      itemView.setOnClickListener { listener.onBookmarkClicked(bookmarks[adapterPosition]) }
    }
  }
}

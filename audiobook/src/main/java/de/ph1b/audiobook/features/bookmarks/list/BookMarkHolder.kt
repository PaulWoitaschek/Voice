package de.ph1b.audiobook.features.bookmarks.list

import de.ph1b.audiobook.misc.layoutInflater


/**
 * ViewHolder for displaying a Bookmark
 */
class BookMarkHolder private constructor(
    private val binding: de.ph1b.audiobook.databinding.BookmarkRowLayoutBinding,
    private val listener: BookmarkClickListener
) : android.support.v7.widget.RecyclerView.ViewHolder(binding.root) {

  private var boundBookmark: de.ph1b.audiobook.Bookmark? = null

  init {
    binding.edit.setOnClickListener { view ->
      boundBookmark?.let {
        listener.onOptionsMenuClicked(it, view)
      }
    }
    itemView.setOnClickListener {
      boundBookmark?.let {
        listener.onBookmarkClicked(it)
      }
    }
  }

  fun bind(bookmark: de.ph1b.audiobook.Bookmark, chapters: List<de.ph1b.audiobook.Chapter>) {
    boundBookmark = bookmark
    binding.title.text = bookmark.title

    val size = chapters.size
    val currentChapter = chapters.single { it.file == bookmark.mediaFile }
    val index = chapters.indexOf(currentChapter)

    binding.summary.text = itemView.context.getString(de.ph1b.audiobook.R.string.format_bookmarks_n_of, index + 1, size)
    binding.time.text = itemView.context.getString(de.ph1b.audiobook.R.string.format_bookmarks_time, formatTime(bookmark.time),
        formatTime(currentChapter.duration))
  }

  private fun formatTime(ms: Int): String {
    val h = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(ms.toLong()).toString()
    val m = "%02d".format((java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(ms.toLong()) % 60))
    val s = "%02d".format((java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60))
    var returnString = ""
    if (h != "0") {
      returnString += h + ":"
    }
    returnString += m + ":" + s
    return returnString
  }

  companion object {

    operator fun invoke(parent: android.view.ViewGroup, listener: BookmarkClickListener): de.ph1b.audiobook.features.bookmarks.list.BookMarkHolder {
      val layoutInflater = parent.layoutInflater()
      val binding = de.ph1b.audiobook.databinding.BookmarkRowLayoutBinding.inflate(layoutInflater, parent, false)
      return de.ph1b.audiobook.features.bookmarks.list.BookMarkHolder(binding, listener)
    }
  }
}

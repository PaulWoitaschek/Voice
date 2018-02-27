package de.ph1b.audiobook.features.bookmarks.list

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.databinding.BookmarkRowLayoutBinding
import de.ph1b.audiobook.misc.layoutInflater
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * ViewHolder for displaying a Bookmark
 */
class BookMarkHolder private constructor(
  private val binding: BookmarkRowLayoutBinding,
  private val listener: BookmarkClickListener
) : RecyclerView.ViewHolder(binding.root) {

  constructor(parent: ViewGroup, listener: BookmarkClickListener) : this(
    binding = BookmarkRowLayoutBinding.inflate(parent.layoutInflater(), parent, false),
    listener = listener
  )

  var boundBookmark: Bookmark? = null
    private set

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

  fun bind(bookmark: Bookmark, chapters: List<Chapter>) {
    boundBookmark = bookmark
    binding.title.text = bookmark.title

    val size = chapters.size
    val currentChapter = chapters.single { it.file == bookmark.mediaFile }
    val index = chapters.indexOf(currentChapter)

    binding.summary.text = itemView.context.getString(
      de.ph1b.audiobook.R.string.format_bookmarks_n_of,
      index + 1,
      size
    )
    binding.time.text = itemView.context.getString(
      de.ph1b.audiobook.R.string.format_bookmarks_time, formatTime(bookmark.time),
      formatTime(currentChapter.duration)
    )
  }

  private fun formatTime(ms: Int): String {
    val h = MILLISECONDS.toHours(ms.toLong()).toString()
    val m = "%02d".format((MILLISECONDS.toMinutes(ms.toLong()) % 60))
    val s = "%02d".format((MILLISECONDS.toSeconds(ms.toLong()) % 60))
    var returnString = ""
    if (h != "0") {
      returnString += h + ":"
    }
    return "$returnString$m:$s"
  }
}

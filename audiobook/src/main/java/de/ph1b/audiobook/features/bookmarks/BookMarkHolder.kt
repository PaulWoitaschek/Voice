package de.ph1b.audiobook.features.bookmarks

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.DialogBookmarkRowLayoutBinding
import de.ph1b.audiobook.misc.layoutInflater
import java.util.concurrent.TimeUnit


/**
 * ViewHolder for displaying a Bookmark
 *
 * @author Paul Woitaschek
 */
class BookMarkHolder private constructor(
    private val binding: DialogBookmarkRowLayoutBinding,
    private val listener: BookMarkClickListener
) : RecyclerView.ViewHolder(binding.root) {

  private var boundBookmark: Bookmark? = null

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

    binding.summary.text = itemView.context.getString(R.string.format_bookmarks_n_of, index + 1, size)
    binding.time.text = itemView.context.getString(R.string.format_bookmarks_time, formatTime(bookmark.time),
        formatTime(currentChapter.duration))
  }

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

  companion object {

    operator fun invoke(parent: ViewGroup, listener: BookMarkClickListener): BookMarkHolder {
      val layoutInflater = parent.layoutInflater()
      val binding = DialogBookmarkRowLayoutBinding.inflate(layoutInflater)
      return BookMarkHolder(binding, listener)
    }
  }
}

package de.ph1b.audiobook.features.bookmarks.list

import android.text.format.DateUtils
import android.view.ViewGroup
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Bookmark2
import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.markForPosition
import de.ph1b.audiobook.databinding.BookmarkRowLayoutBinding
import de.ph1b.audiobook.uitools.ViewBindingHolder
import voice.common.formatTime

class BookMarkHolder(
  parent: ViewGroup,
  private val listener: BookmarkClickListener
) : ViewBindingHolder<BookmarkRowLayoutBinding>(parent, BookmarkRowLayoutBinding::inflate) {

  var boundBookmark: Bookmark2? = null
    private set

  init {
    binding.edit.setOnClickListener { view ->
      boundBookmark?.let {
        listener.onOptionsMenuClicked(it, view)
      }
    }
    itemView.setOnClickListener {
      boundBookmark?.let { bookmark ->
        listener.onBookmarkClicked(bookmark)
      }
    }
  }

  fun bind(bookmark: Bookmark2, chapters: List<Chapter2>) {
    boundBookmark = bookmark
    val currentChapter = chapters.single { it.id == bookmark.chapterId }
    val bookmarkTitle = bookmark.title
    binding.title.text = when {
      bookmark.setBySleepTimer -> {
        DateUtils.formatDateTime(itemView.context, bookmark.addedAt.toEpochMilli(), DateUtils.FORMAT_SHOW_TIME)
      }
      bookmarkTitle != null && bookmarkTitle.isNotEmpty() -> bookmarkTitle
      else -> currentChapter.markForPosition(bookmark.time).name
    }
    binding.title.setCompoundDrawablesRelativeWithIntrinsicBounds(if (bookmark.setBySleepTimer) R.drawable.ic_sleep else 0, 0, 0, 0)
    val size = chapters.size
    val index = chapters.indexOf(currentChapter)

    binding.summary.text = itemView.context.getString(
      R.string.format_bookmarks_n_of,
      index + 1,
      size
    )
    binding.time.text = formatTime(bookmark.time)
  }
}

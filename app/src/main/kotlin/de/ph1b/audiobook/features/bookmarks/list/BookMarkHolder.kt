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
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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
        val justNowThreshold = 1.minutes
        if (ChronoUnit.MILLIS.between(bookmark.addedAt, Instant.now()).milliseconds < justNowThreshold) {
          itemView.context.getString(R.string.bookmark_just_now)
        } else {
          DateUtils.getRelativeDateTimeString(
            itemView.context,
            bookmark.addedAt.toEpochMilli(),
            justNowThreshold.inWholeMilliseconds,
            2.days.inWholeMilliseconds,
            0
          )
        }
      }
      bookmarkTitle != null && bookmarkTitle.isNotEmpty() -> bookmarkTitle
      else -> currentChapter.markForPosition(bookmark.time).name
    }
    binding.title.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, if (bookmark.setBySleepTimer) R.drawable.ic_sleep else 0, 0)
    binding.time.text = formatTime(bookmark.time)
  }
}

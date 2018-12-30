package de.ph1b.audiobook.features.bookOverview.list.header

import androidx.annotation.StringRes
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.bookOverview.list.BookComparator

enum class BookOverviewCategory(
  @StringRes val nameRes: Int,
  val filter: (Book) -> Boolean,
  val comparator: Comparator<Book>
) {
  CURRENT(
    nameRes = R.string.book_header_current,
    filter = {
      val position = it.content.position
      val duration = it.content.duration
      position in 1 until duration
    },
    comparator = BookComparator.BY_LAST_PLAYED
  ),
  NOT_STARTED(
    nameRes = R.string.book_header_not_started,
    filter = { it.content.position == 0 },
    comparator = BookComparator.BY_DATE_ADDED
  ),
  FINISHED(
    nameRes = R.string.book_header_completed,
    filter = { it.content.position >= it.content.duration },
    comparator = BookComparator.BY_LAST_PLAYED
  )
}

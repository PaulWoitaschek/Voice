package de.ph1b.audiobook.features.bookOverview.list.header

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.bookOverview.list.BookComparator

enum class BookOverviewCategory(val filter: (Book) -> Boolean, val comparator: Comparator<Book>) {
  CURRENT(
    filter = {
      val position = it.content.position
      val duration = it.content.duration
      position in 1 until duration
    },
    comparator = BookComparator.BY_LAST_PLAYED
  ),
  NOT_STARTED(
    filter = { it.content.position == 0 },
    comparator = BookComparator.BY_DATE_ADDED
  ),
  FINISHED(
    filter = { it.content.position >= it.content.duration },
    comparator = BookComparator.BY_LAST_PLAYED
  )
}

package de.ph1b.audiobook.features.bookOverview.list.header

import androidx.annotation.StringRes
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookComparator
import java.util.concurrent.TimeUnit.SECONDS

enum class BookOverviewCategory(
  @StringRes val nameRes: Int,
  val comparator: Comparator<Book2>
) {
  CURRENT(
    nameRes = R.string.book_header_current,
    comparator = BookComparator.BY_LAST_PLAYED
  ),
  NOT_STARTED(
    nameRes = R.string.book_header_not_started,
    comparator = BookComparator.BY_DATE_ADDED
  ),
  FINISHED(
    nameRes = R.string.book_header_completed,
    comparator = BookComparator.BY_LAST_PLAYED
  );

  val filter: (Book2) -> Boolean = { it.category == this }
}

val Book2.category: BookOverviewCategory
  get() {
    return if (position == 0L) {
      BookOverviewCategory.NOT_STARTED
    } else {
      if (position > duration - SECONDS.toMillis(1)) {
        BookOverviewCategory.FINISHED
      } else {
        BookOverviewCategory.CURRENT
      }
    }
  }

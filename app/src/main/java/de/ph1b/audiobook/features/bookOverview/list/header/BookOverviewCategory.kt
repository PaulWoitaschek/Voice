package de.ph1b.audiobook.features.bookOverview.list.header

import androidx.annotation.StringRes
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookComparator
import java.util.concurrent.TimeUnit.SECONDS

enum class BookOverviewCategory(
  @StringRes val nameRes: Int,
  val comparator: Comparator<Book>
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

  val filter: (Book) -> Boolean = { it.category == this }
}

val Book.category: BookOverviewCategory
  get() {
    val position = content.position
    return if (position == 0L) {
      BookOverviewCategory.NOT_STARTED
    } else {
      val duration = content.duration
      if (position > duration - SECONDS.toMillis(1)) {
        BookOverviewCategory.FINISHED
      } else {
        BookOverviewCategory.CURRENT
      }
    }
  }

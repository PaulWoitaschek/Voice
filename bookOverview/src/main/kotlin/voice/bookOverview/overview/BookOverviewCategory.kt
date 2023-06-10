package voice.bookOverview.overview

import androidx.annotation.StringRes
import voice.data.Book
import voice.data.BookComparator
import java.util.concurrent.TimeUnit.SECONDS
import voice.strings.R as StringsR

enum class BookOverviewCategory(
  @StringRes val nameRes: Int,
  val comparator: Comparator<Book>,
) {
  CURRENT(
    nameRes = StringsR.string.book_header_current,
    comparator = BookComparator.ByLastPlayed,
  ),
  NOT_STARTED(
    nameRes = StringsR.string.book_header_not_started,
    comparator = BookComparator.ByName,
  ),
  FINISHED(
    nameRes = StringsR.string.book_header_completed,
    comparator = BookComparator.ByLastPlayed,
  ),
}

val Book.category: BookOverviewCategory
  get() {
    return if (position == 0L) {
      BookOverviewCategory.NOT_STARTED
    } else {
      if (position >= duration - SECONDS.toMillis(5)) {
        BookOverviewCategory.FINISHED
      } else {
        BookOverviewCategory.CURRENT
      }
    }
  }

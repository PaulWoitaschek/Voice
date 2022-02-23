package voice.bookOverview

import androidx.annotation.StringRes
import voice.data.Book
import voice.data.BookComparator
import java.util.concurrent.TimeUnit.SECONDS

enum class BookOverviewCategory(
  @StringRes val nameRes: Int,
  val comparator: Comparator<Book>
) {
  CURRENT(
    nameRes = R.string.book_header_current,
    comparator = BookComparator.ByLastPlayed
  ),
  NOT_STARTED(
    nameRes = R.string.book_header_not_started,
    comparator = BookComparator.ByLastPlayed.then(BookComparator.ByDateAdded).then(BookComparator.ByName)
  ),
  FINISHED(
    nameRes = R.string.book_header_completed,
    comparator = BookComparator.ByLastPlayed
  );
}

val Book.category: BookOverviewCategory
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

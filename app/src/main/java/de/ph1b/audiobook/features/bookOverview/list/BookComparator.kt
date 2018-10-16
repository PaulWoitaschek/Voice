package de.ph1b.audiobook.features.bookOverview.list

import androidx.annotation.StringRes
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.data.Book

enum class BookComparator(
  @StringRes val nameId: Int,
  val comparatorFunction: Comparator<Book>
) {
  BY_LAST_PLAYED(R.string.pref_sort_by_last_played, compareByDescending {
    it.content.settings.lastPlayedAtMillis
  }),
  BY_NAME(
    R.string.pref_sort_by_name,
    BookComparator.BY_LAST_PLAYED.comparatorFunction.then(Comparator { o1, o2 ->
      NaturalOrderComparator.stringComparator.compare(o1.name, o2.name)
    })
  ),
  BY_DATE_ADDED(R.string.pref_sort_by_date_added, compareBy { it.metaData.addedAtMillis });
}

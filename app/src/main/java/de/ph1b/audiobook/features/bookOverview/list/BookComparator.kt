package de.ph1b.audiobook.features.bookOverview.list

import androidx.annotation.StringRes
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.data.Book

private val byName = Comparator<Book> { left, right ->
  NaturalOrderComparator.stringComparator.compare(left.name, right.name)
}
private val byLastPlayed = compareByDescending<Book> {
  it.content.settings.lastPlayedAtMillis
}

enum class BookComparator(
  @StringRes val nameId: Int,
  private val comparatorFunction: Comparator<Book>
) : Comparator<Book> by comparatorFunction {
  BY_LAST_PLAYED(R.string.pref_sort_by_last_played, (byLastPlayed).then(byName)),
  BY_NAME(R.string.pref_sort_by_name, byName.then(byLastPlayed)),
  BY_DATE_ADDED(
    R.string.pref_sort_by_date_added,
    compareByDescending<Book> { it.metaData.addedAtMillis }.then(byName)
  );
}

package de.ph1b.audiobook.data

import de.ph1b.audiobook.common.comparator.NaturalOrderComparator

private val byName = Comparator<BookContent2> { left, right ->
  NaturalOrderComparator.stringComparator.compare(left.name, right.name)
}
private val byLastPlayed = compareByDescending<BookContent2> {
  it.lastPlayedAt
}

private val byAddedAt = compareByDescending<BookContent2> { it.addedAt }

enum class BookComparator(private val comparatorFunction: Comparator<BookContent2>) :
  Comparator<BookContent2> by comparatorFunction {
  BY_LAST_PLAYED((byLastPlayed).then(byName)),
  BY_NAME(byName.then(byLastPlayed)),
  BY_DATE_ADDED(byAddedAt.then(byName));
}

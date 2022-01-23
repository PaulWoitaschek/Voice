package de.ph1b.audiobook.data

import de.ph1b.audiobook.common.comparator.NaturalOrderComparator

private val byName = Comparator<Book2> { left, right ->
  NaturalOrderComparator.stringComparator.compare(left.content.name, right.content.name)
}
private val byLastPlayed = compareByDescending<Book2> {
  it.content.lastPlayedAt
}

private val byAddedAt = compareByDescending<Book2> { it.content.addedAt }

enum class BookComparator(private val comparatorFunction: Comparator<Book2>) :
  Comparator<Book2> by comparatorFunction {
  BY_LAST_PLAYED((byLastPlayed).then(byName)),
  BY_NAME(byName.then(byLastPlayed)),
  BY_DATE_ADDED(byAddedAt.then(byName));
}

package de.ph1b.audiobook.features.bookOverview.list

import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.data.Book

val BY_NAME = Comparator<Book> { o1, o2 ->
  NaturalOrderComparator.stringComparator.compare(o1.name, o2.name)
}

val BY_LAST_PLAYED = compareByDescending<Book> {
  it.content.settings.lastPlayedAtMillis
}
val BY_LAST_PLAYED_THEN_NAME: Comparator<Book> = BY_LAST_PLAYED.then(BY_NAME)

package voice.core.data

import voice.core.common.comparator.NaturalOrderComparator

public enum class BookComparator(private val comparatorFunction: Comparator<Book>) : Comparator<Book> by comparatorFunction {

  ByLastPlayed(
    compareByDescending {
      it.content.lastPlayedAt
    },
  ),
  ByName(
    Comparator { left, right ->
      NaturalOrderComparator.stringComparator.compare(left.content.name, right.content.name)
    },
  ),
}

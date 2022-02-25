package voice.data

import voice.common.comparator.NaturalOrderComparator

enum class BookComparator(
  private val comparatorFunction: Comparator<Book>
) : Comparator<Book> by comparatorFunction {

  ByLastPlayed(compareByDescending<Book> {
    it.content.lastPlayedAt
  }),
  ByName(Comparator<Book> { left, right ->
    NaturalOrderComparator.stringComparator.compare(left.content.name, right.content.name)
  }),
  ByDateAdded(compareByDescending<Book> { it.content.addedAt })
}

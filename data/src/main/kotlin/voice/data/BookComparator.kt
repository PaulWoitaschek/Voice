package voice.data

import voice.common.comparator.NaturalOrderComparator

private val byName = Comparator<Book> { left, right ->
  NaturalOrderComparator.stringComparator.compare(left.content.name, right.content.name)
}
private val byLastPlayed = compareByDescending<Book> {
  it.content.lastPlayedAt
}

private val byAddedAt = compareByDescending<Book> { it.content.addedAt }

enum class BookComparator(
  private val comparatorFunction: Comparator<Book>
) : Comparator<Book> by comparatorFunction {

  ByLastPlayed((byLastPlayed).then(byName)),
  ByName(byName.then(byLastPlayed)),
  ByDateAdded(byAddedAt.then(byName))
}

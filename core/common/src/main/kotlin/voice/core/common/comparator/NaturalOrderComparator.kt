package voice.core.common.comparator

object NaturalOrderComparator {

  val stringComparator: Comparator<String> = NaturalComparator
}

fun Set<String>.sortedNaturally(): List<String> = sortedWith(NaturalOrderComparator.stringComparator)

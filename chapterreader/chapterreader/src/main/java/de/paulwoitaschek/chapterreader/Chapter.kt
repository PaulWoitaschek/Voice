package de.paulwoitaschek.chapterreader

data class Chapter(
  val startInMs: Long,
  val title: String
) : Comparable<Chapter> {

  override fun compareTo(other: Chapter): Int = compareValuesBy(this, other, { it.startInMs }, { it.title })
}

package de.ph1b.audiobook.data

import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import java.io.File

/**
 * Represents a bookmark in the book.
 */
data class Bookmark(
  val mediaFile: File,
  val title: String,
  val time: Int,
  val id: Long = ID_UNKNOWN
) : Comparable<Bookmark> {

  init {
    require(title.isNotEmpty())
  }

  override fun compareTo(other: Bookmark): Int {
    // compare files
    val fileCompare = NaturalOrderComparator.fileComparator.compare(mediaFile, other.mediaFile)
    if (fileCompare != 0) {
      return fileCompare
    }

    // if files are the same compare time
    val timeCompare = time.compareTo(other.time)
    if (timeCompare != 0) return timeCompare

    // if time is the same compare the titles
    val titleCompare = NaturalOrderComparator.stringComparator.compare(title, other.title)
    if (titleCompare != 0) return titleCompare

    return id.compareTo(other.id)
  }

  companion object {
    val ID_UNKNOWN = -1L
  }
}

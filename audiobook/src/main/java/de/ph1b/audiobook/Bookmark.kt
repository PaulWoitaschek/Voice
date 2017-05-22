package de.ph1b.audiobook

import de.ph1b.audiobook.misc.NaturalOrderComparator
import java.io.File

/**
 * Represents a bookmark in the book.
 *
 * @author Paul Woitaschek
 */
data class Bookmark(val mediaFile: File, val title: String, val time: Int, val id: Long = ID_UNKNOWN) : Comparable<Bookmark> {

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
    val timeCompare = other.time.compareTo(time)
    if (timeCompare != 0) return timeCompare

    // if time is the same compare the titles
    return NaturalOrderComparator.stringComparator.compare(title, other.title)
  }

  companion object {
    val ID_UNKNOWN = -1L
  }
}
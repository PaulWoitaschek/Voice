package de.ph1b.audiobook.data

import android.support.v4.util.SparseArrayCompat
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.common.sparseArray.contentEquals
import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import java.io.File

/**
 * Represents a chapter in a book.
 */
data class Chapter(
  val file: File,
  val name: String,
  val duration: Int,
  val fileLastModified: Long,
  val marks: SparseArrayCompat<String>
) : Comparable<Chapter> {

  init {
    require(name.isNotEmpty())
  }

  override fun compareTo(other: Chapter) =
    NaturalOrderComparator.fileComparator.compare(file, other.file)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Chapter) return false
    return this.file == other.file
        && this.name == other.name
        && this.duration == other.duration
        && this.fileLastModified == other.fileLastModified
        && this.marks.contentEquals(other.marks)
  }

  override fun hashCode(): Int {
    var hashCode = 17
    hashCode = 31 * hashCode + file.hashCode()
    hashCode = 31 * hashCode + name.hashCode()
    hashCode = 31 * hashCode + duration.hashCode()
    hashCode = 31 * hashCode + fileLastModified.hashCode()
    marks.forEachIndexed { index, key, value ->
      hashCode = 31 * hashCode + index.hashCode()
      hashCode = 31 * hashCode + key.hashCode()
      hashCode = 31 * hashCode + value.hashCode()
    }
    return hashCode
  }
}

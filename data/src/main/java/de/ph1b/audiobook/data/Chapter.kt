package de.ph1b.audiobook.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.support.v4.util.SparseArrayCompat
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.common.sparseArray.contentEquals
import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import java.io.File

/**
 * Represents a chapter in a book.
 */
@Entity(
  tableName = "chapters",
  indices = [(Index(value = ["bookId"]))]
)
data class Chapter(
  @ColumnInfo(name = "file")
  val file: File,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "duration")
  val duration: Int,
  @ColumnInfo(name = "fileLastModified")
  val fileLastModified: Long,
  @ColumnInfo(name = "marks")
  val marks: SparseArrayCompat<String>,
  @ColumnInfo(name = "id")
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  @ColumnInfo(name = "bookId")
  val bookId: Long = 0L
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

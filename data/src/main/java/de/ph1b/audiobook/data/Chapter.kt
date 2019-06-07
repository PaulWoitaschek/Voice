package de.ph1b.audiobook.data

import androidx.collection.SparseArrayCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.common.sparseArray.contentEquals
import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import java.io.File
import java.util.UUID

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
  val duration: Long,
  @ColumnInfo(name = "fileLastModified")
  val fileLastModified: Long,
  @ColumnInfo(name = "marks")
  val marks: SparseArrayCompat<String>,
  @ColumnInfo(name = "bookId")
  val bookId: UUID,
  @ColumnInfo(name = "id")
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L
) : Comparable<Chapter> {

  init {
    require(name.isNotEmpty())
  }

  override fun compareTo(other: Chapter) =
    NaturalOrderComparator.fileComparator.compare(file, other.file)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Chapter) return false
    return this.file == other.file &&
        this.name == other.name &&
        this.duration == other.duration &&
        this.fileLastModified == other.fileLastModified &&
        this.marks.contentEquals(other.marks)
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

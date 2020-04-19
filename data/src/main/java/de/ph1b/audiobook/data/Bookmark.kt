package de.ph1b.audiobook.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import org.threeten.bp.Instant
import java.io.File
import java.util.UUID

@Entity(tableName = "bookmark")
data class Bookmark(
  @ColumnInfo(name = "file")
  val mediaFile: File,
  @ColumnInfo(name = "title")
  val title: String?,
  @ColumnInfo(name = "time")
  val time: Long,
  @ColumnInfo(name = "addedAt")
  val addedAt: Instant,
  @ColumnInfo(name = "setBySleepTimer")
  val setBySleepTimer: Boolean,
  @ColumnInfo(name = "id")
  @PrimaryKey
  val id: UUID
) : Comparable<Bookmark> {

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
}

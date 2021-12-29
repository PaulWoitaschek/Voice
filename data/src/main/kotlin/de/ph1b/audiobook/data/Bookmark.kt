package de.ph1b.audiobook.data

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import java.io.File
import java.time.Instant
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

  @Ignore
  val mediaUri: Uri = mediaFile.toUri()

  override fun compareTo(other: Bookmark): Int {
    // compare uri
    val uriCompare = NaturalOrderComparator.uriComparator.compare(mediaUri, other.mediaUri)
    if (uriCompare != 0) {
      return uriCompare
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

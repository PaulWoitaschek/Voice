package voice.data.legacy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.time.Instant
import java.util.UUID

@Entity(tableName = "bookmark")
data class LegacyBookmark(
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
)

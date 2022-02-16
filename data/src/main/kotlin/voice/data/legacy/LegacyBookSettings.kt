package voice.data.legacy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.util.UUID

@Entity(tableName = "bookSettings")
data class LegacyBookSettings(
  @ColumnInfo(name = "id")
  @PrimaryKey
  val id: UUID,
  @ColumnInfo(name = "currentFile")
  val currentFile: File,
  @ColumnInfo(name = "positionInChapter")
  val positionInChapter: Long,
  @ColumnInfo(name = "playbackSpeed")
  val playbackSpeed: Float = 1F,
  @ColumnInfo(name = "loudnessGain")
  val loudnessGain: Int = 0,
  @ColumnInfo(name = "skipSilence")
  val skipSilence: Boolean = false,
  @ColumnInfo(name = "active")
  val active: Boolean,
  @ColumnInfo(name = "lastPlayedAtMillis")
  val lastPlayedAtMillis: Long
)

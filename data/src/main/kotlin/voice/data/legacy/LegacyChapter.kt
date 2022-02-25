package voice.data.legacy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import voice.data.MarkData
import java.io.File
import java.util.UUID

@Entity(
  tableName = "chapters",
  indices = [(Index(value = ["bookId"]))]
)
data class LegacyChapter(
  @ColumnInfo(name = "file")
  val file: File,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "duration")
  val duration: Long,
  @ColumnInfo(name = "fileLastModified")
  val fileLastModified: Long,
  @ColumnInfo(name = "marks")
  val markData: List<MarkData>,
  @ColumnInfo(name = "bookId")
  val bookId: UUID,
  @ColumnInfo(name = "id")
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L
)

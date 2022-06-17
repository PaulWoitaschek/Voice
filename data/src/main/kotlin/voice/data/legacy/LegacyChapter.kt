package voice.data.legacy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
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
  val markData: List<LegacyMarkData>,
  @ColumnInfo(name = "bookId")
  val bookId: UUID,
  @ColumnInfo(name = "id")
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L
) {

  @Ignore
  val chapterMarks: List<LegacyChapterMark> = if (markData.isEmpty()) {
    listOf(LegacyChapterMark(name, 0L, duration))
  } else {
    val sorted = markData.sorted()
    sorted.mapIndexed { index, (startMs, name) ->
      val isFirst = index == 0
      val isLast = index == sorted.size - 1
      val start = if (isFirst) 0L else startMs
      val end = if (isLast) duration else sorted[index + 1].startMs - 1
      LegacyChapterMark(name = name, startMs = start, endMs = end)
    }
  }

  fun markForPosition(positionInChapterMs: Long): LegacyChapterMark {
    return chapterMarks.find { positionInChapterMs in it.startMs..it.endMs }
      ?: chapterMarks.firstOrNull { positionInChapterMs == it.endMs }
      ?: chapterMarks.first()
  }
}

package de.ph1b.audiobook.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import kotlinx.serialization.Serializable
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
  val markData: List<MarkData>,
  @ColumnInfo(name = "bookId")
  val bookId: UUID,
  @ColumnInfo(name = "id")
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L
) : Comparable<Chapter> {

  init {
    require(name.isNotEmpty())
  }

  @Ignore
  val chapterMarks: List<ChapterMark>

  init {
    chapterMarks = if (markData.isEmpty()) {
      listOf(ChapterMark(name, 0L, duration))
    } else {
      val sorted = markData.sorted()
      sorted.mapIndexed { index, (startMs, name) ->
        val isFirst = index == 0
        val isLast = index == sorted.size - 1
        val start = if (isFirst) 0L else startMs
        val end = if (isLast) duration else sorted[index + 1].startMs - 1
        ChapterMark(name = name, startMs = start, endMs = end)
      }
    }
  }

  override fun compareTo(other: Chapter): Int = NaturalOrderComparator.fileComparator.compare(file, other.file)
}

@Serializable
data class MarkData(
  val startMs: Long,
  val name: String
) : Comparable<MarkData> {
  override fun compareTo(other: MarkData): Int {
    return startMs.compareTo(other.startMs)
  }
}

class ChapterMark(
  val name: String,
  val startMs: Long,
  val endMs: Long
)

val ChapterMark.durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

fun Chapter.markForPosition(positionInChapterMs: Long): ChapterMark {
  return chapterMarks.find { positionInChapterMs in it.startMs..it.endMs }
    ?: chapterMarks.firstOrNull { positionInChapterMs == it.endMs }
    ?: chapterMarks.first()
}

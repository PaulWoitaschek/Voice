package voice.data

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class MarkData(
  val startMs: Long,
  val name: String,
) : Comparable<MarkData> {
  override fun compareTo(other: MarkData): Int {
    return startMs.compareTo(other.startMs)
  }
}

@Serializable
data class ChapterMark(
  val name: String?,
  val startMs: Long,
  val endMs: Long,
) {

  operator fun contains(position: Duration): Boolean = position.inWholeMilliseconds in startMs..endMs
  operator fun contains(positionMs: Long): Boolean = positionMs in startMs..endMs
}

val ChapterMark.durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

fun Chapter.markForPosition(positionInChapterMs: Long): ChapterMark {
  return chapterMarks.find { positionInChapterMs in it.startMs..it.endMs }
    ?: chapterMarks.firstOrNull { positionInChapterMs == it.endMs }
    ?: chapterMarks.first()
}

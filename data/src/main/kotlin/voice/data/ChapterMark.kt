package voice.data

import kotlinx.serialization.Serializable

@Serializable
data class MarkData(
  val startMs: Long,
  val name: String,
) : Comparable<MarkData> {
  override fun compareTo(other: MarkData): Int {
    return startMs.compareTo(other.startMs)
  }
}

data class ChapterMark(
  val name: String?,
  val startMs: Long,
  val endMs: Long,
)

val ChapterMark.durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

fun Chapter.markForPosition(positionInChapterMs: Long): ChapterMark {
  return chapterMarks.find { positionInChapterMs in it.startMs..it.endMs }
    ?: chapterMarks.firstOrNull { positionInChapterMs == it.endMs }
    ?: chapterMarks.first()
}

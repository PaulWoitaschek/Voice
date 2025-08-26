package voice.core.data

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
public data class MarkData(
  val startMs: Long,
  val name: String,
) : Comparable<MarkData> {
  override fun compareTo(other: MarkData): Int {
    return startMs.compareTo(other.startMs)
  }
}

@Serializable
public data class ChapterMark(
  val name: String?,
  val startMs: Long,
  val endMs: Long,
) {

  init {
    require(startMs < endMs) {
      "Start must be less than end in $this"
    }
  }

  public operator fun contains(position: Duration): Boolean = position.inWholeMilliseconds in startMs..endMs
  public operator fun contains(positionMs: Long): Boolean = positionMs in startMs..endMs
}

public val ChapterMark.durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

public fun Chapter.markForPosition(positionInChapterMs: Long): ChapterMark {
  return chapterMarks.find { positionInChapterMs in it.startMs..it.endMs }
    ?: chapterMarks.firstOrNull { positionInChapterMs == it.endMs }
    ?: chapterMarks.first()
}

package voice.data.legacy

import kotlinx.serialization.Serializable

@Serializable
public data class LegacyMarkData(
  val startMs: Long,
  val name: String,
) : Comparable<LegacyMarkData> {
  override fun compareTo(other: LegacyMarkData): Int {
    return startMs.compareTo(other.startMs)
  }
}

public data class LegacyChapterMark(
  val name: String,
  val startMs: Long,
  val endMs: Long,
)

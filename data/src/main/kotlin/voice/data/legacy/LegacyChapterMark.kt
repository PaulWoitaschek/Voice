package voice.data.legacy

import kotlinx.serialization.Serializable

@Serializable
data class LegacyMarkData(
  val startMs: Long,
  val name: String,
) : Comparable<LegacyMarkData> {
  override fun compareTo(other: LegacyMarkData): Int {
    return startMs.compareTo(other.startMs)
  }
}

data class LegacyChapterMark(
  val name: String,
  val startMs: Long,
  val endMs: Long,
)

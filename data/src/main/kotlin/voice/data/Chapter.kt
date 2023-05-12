package voice.data

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Entity(tableName = "chapters2")
data class Chapter(
  @PrimaryKey
  val id: ChapterId,
  val name: String?,
  val duration: Long,
  val fileLastModified: Instant,
  val markData: List<MarkData>,
) : Comparable<Chapter> {

  @Ignore
  val chapterMarks: List<ChapterMark> = if (markData.isEmpty()) {
    listOf(ChapterMark(name, 0L, duration))
  } else {
    val sorted = markData.distinctBy { it.startMs }
      .filter { it.startMs in 0..<duration }
      .sorted()
    val result = mutableListOf<ChapterMark>()
    for ((index, markData) in sorted.withIndex()) {
      val name = markData.name
      val previous = result.lastOrNull()
      val next = sorted.getOrNull(index + 1)
      val startMs = if (previous == null) 0L else previous.endMs + 1
      val endMs = if (next != null && next.startMs + 2 < duration && startMs < next.startMs - 1) {
        next.startMs - 1
      } else {
        duration - 1
      }
      result += ChapterMark(
        name = name,
        startMs = startMs,
        endMs = endMs,
      )
      if (endMs == duration - 1) {
        break
      }
    }
    result.ifEmpty { listOf(ChapterMark(name, 0L, duration)) }
  }

  override fun compareTo(other: Chapter): Int {
    return id.compareTo(other.id)
  }
}

object ChapterIdSerializer : KSerializer<ChapterId> {

  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("chapterId", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): ChapterId = ChapterId(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: ChapterId) {
    encoder.encodeString(value.value)
  }
}

fun ChapterId.toUri(): Uri = value.toUri()

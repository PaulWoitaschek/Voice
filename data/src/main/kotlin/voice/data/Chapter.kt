package voice.data

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Entity(tableName = "chapters2")
data class Chapter(
  @PrimaryKey
  val id: Id,
  val name: String,
  val duration: Long,
  val fileLastModified: Instant,
  val markData: List<MarkData>,
) {

  init {
    require(name.isNotEmpty())
  }

  @Ignore
  val chapterMarks: List<ChapterMark> = if (markData.isEmpty()) {
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

  @Serializable(with = ChapterIdSerializer::class)
  data class Id(val value: String) {

    constructor(uri: Uri) : this(uri.toString())
  }
}

object ChapterIdSerializer : KSerializer<Chapter.Id> {

  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("chapterId", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Chapter.Id = Chapter.Id(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: Chapter.Id) {
    encoder.encodeString(value.value)
  }
}


fun Chapter.Id.toUri(): Uri = value.toUri()

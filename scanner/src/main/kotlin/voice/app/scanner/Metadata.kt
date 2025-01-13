package voice.app.scanner

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
internal data class MetaDataScanResult(
  val streams: List<MetaDataStream> = emptyList(),
  val chapters: List<MetaDataChapter> = emptyList(),
  val format: MetaDataFormat? = null,
)

internal enum class TagType(val keys: List<String>) {
  Title(listOf("title")),
  Artist(listOf("author", "artist", "album_artist")),
  Album(listOf("album")),
}

internal fun MetaDataScanResult.findTag(tagType: TagType): String? {
  format?.tags?.find(tagType)?.let { return it }
  streams.forEach { stream ->
    stream.tags?.find(tagType)?.let { return it }
  }
  chapters.forEach { chapter ->
    chapter.tags?.find(tagType)?.let { return it }
  }
  return null
}

internal fun Map<String, String>.find(tagType: TagType): String? {
  forEach { (key, value) ->
    tagType.keys.forEach { targetKey ->
      if (key.equals(targetKey, ignoreCase = true) && value.isNotEmpty()) {
        return value
      }
    }
  }
  return null
}

@Serializable
internal data class MetaDataStream(val tags: Map<String, String>? = null)

@Serializable
internal data class MetaDataChapter(
  @SerialName("start_time")
  private val startInSeconds: Double,
  val tags: Map<String, String>? = null,
) {

  val start: Duration get() = startInSeconds.seconds
}

@Serializable
internal data class MetaDataFormat(
  val duration: Double? = null,
  val tags: Map<String, String>? = null,
)

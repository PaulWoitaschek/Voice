package de.ph1b.audiobook.misc.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.seconds

@Serializable
data class MetaDataScanResult(
  val streams: List<MetaDataStream>,
  val chapters: List<MetaDataChapter>,
  val format: MetaDataFormat?
)

enum class TagType {
  Title, Artist, Album
}

fun MetaDataScanResult.findTag(tagType: TagType): String? {
  format?.tags?.find(tagType)?.let { return it }
  streams.forEach { stream ->
    stream.tags?.find(tagType)?.let { return it }
  }
  chapters.forEach { chapter ->
    chapter.tags?.find(tagType)?.let { return it }
  }
  return null
}

fun Map<String, String>.find(tagType: TagType): String? {
  val targetKey = when (tagType) {
    TagType.Title -> "title"
    TagType.Artist -> "artist"
    TagType.Album -> "album"
  }
  forEach { (key, value) ->
    if (key.equals(targetKey, ignoreCase = true) && value.isNotEmpty()) {
      return value
    }
  }
  return null
}

@Serializable
data class MetaDataStream(
  val tags: Map<String, String>? = null
)

@Serializable
data class MetaDataChapter(
  @SerialName("start_time")
  private val startInSeconds: Double,
  val tags: Map<String, String>? = null
) {

  val start: Duration get() = startInSeconds.seconds
}

@Serializable
data class MetaDataFormat(
  val duration: Double? = null,
  val tags: Map<String, String>? = null
)

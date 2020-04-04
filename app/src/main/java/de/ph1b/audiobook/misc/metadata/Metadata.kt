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
  val tags = mutableListOf<MetaDataTags>()
  format?.tags?.let { tags += it }
  streams.mapNotNullTo(tags) { it.tags }
  chapters.mapNotNullTo(tags) { it.tags }
  tags.forEach { tag ->
    val result = tag.find(tagType)
    if (result != null) {
      return result
    }
  }
  return null
}

private fun MetaDataTags.find(tagType: TagType): String? {
  return when (tagType) {
    TagType.Title -> title
    TagType.Artist -> artist
    TagType.Album -> album
  }?.takeIf { it.isNotEmpty() }
}

@Serializable
data class MetaDataStream(
  val tags: MetaDataTags? = null
)

@Serializable
data class MetaDataChapter(
  @SerialName("start_time")
  private val startInSeconds: Double,
  val tags: MetaDataTags? = null
) {

  val start: Duration get() = startInSeconds.seconds
}

@Serializable
data class MetaDataFormat(
  val duration: Double? = null,
  val tags: MetaDataTags? = null
)

@Serializable
data class MetaDataTags(
  val title: String? = null,
  val artist: String? = null,
  val album: String? = null
)

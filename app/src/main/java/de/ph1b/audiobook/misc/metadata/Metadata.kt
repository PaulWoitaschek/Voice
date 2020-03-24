package de.ph1b.audiobook.misc.metadata

import kotlinx.serialization.Serializable

@Serializable
data class MetaDataScanResult(
  val streams: List<MetaDataStream>,
  val chapters: List<MetaDataChapter>,
  val format: MetaDataFormat?
)

inline fun MetaDataScanResult.findTag(find: MetaDataTags.() -> String?): String? {
  val formatResult = format?.tags?.find()
  if (formatResult != null) {
    return formatResult
  } else {
    streams.forEach { stream ->
      val streamResult = stream.tags?.find()
      if (streamResult != null) {
        return streamResult
      }
    }
    chapters.forEach { chapter ->
      chapter.tags
    }
  }
  return null
}

@Serializable
data class MetaDataStream(
  val tags: MetaDataTags? = null
)

@Serializable
data class MetaDataChapter(val start: Long, val tags: MetaDataChapterTags? = null)

@Serializable
data class MetaDataChapterTags(
  val title: String? = null
)

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

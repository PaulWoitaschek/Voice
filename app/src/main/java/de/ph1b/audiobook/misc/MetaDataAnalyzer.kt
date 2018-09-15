package de.ph1b.audiobook.misc

import android.media.MediaMetadataRetriever
import java.io.File

/**
 * Extracts meta data from media files. This class is thread safe.
 */
class MetaDataAnalyzer {

  private val mmr = MediaMetadataRetriever()
  private var file: File? = null

  @Synchronized
  fun parse(file: File): MetaData {
    this.file = file
    // try preparing twice as MediaMetadataRetriever throws undocumented exceptions
    val fallback = chapterNameFallback()
    return if (prepare() || prepare()) {
      val chapterName = parseChapterName()
        ?: fallback
      val duration = parseDuration()
      val bookName = parseBookName()
      val author = parseAuthor()
      MetaData(chapterName, duration, bookName, author)
    } else {
      MetaData(fallback, null, null, null)
    }
  }

  private fun prepare(): Boolean {
    // Note: MediaMetadataRetriever throws undocumented Exceptions. We catch these
    // and act appropriate.
    val file = file ?: error("No file")
    return try {
      mmr.setDataSource(file.absolutePath)
      true
    } catch (e: RuntimeException) {
      false
    }
  }

  private fun parseChapterName(): String? {
    val chapterName = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_TITLE)
    if (chapterName?.isEmpty() == true)
      return null
    return chapterName
  }

  private fun chapterNameFallback(): String {
    val file = file ?: error("No file prepared")
    val withoutExtension = file.nameWithoutExtension
      .trim()
    return if (withoutExtension.isEmpty()) file.name else withoutExtension
  }

  private fun parseDuration() = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_DURATION)
    ?.toIntOrNull()
    ?: 0

  private fun parseBookName() = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUM)

  private fun parseAuthor(): String? {
    var author = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ARTIST)
    if (author.isNullOrEmpty())
      author = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
    return author
  }

  /**
   * As [MediaMetadataRetriever] has several bugs it is important to catch the exception here as
   * it randomly throws [RuntimeException] on certain implementations.
   */
  private fun MediaMetadataRetriever.safeExtract(key: Int) = try {
    extractMetadata(key)
  } catch (ignored: Exception) {
    null
  }

  data class MetaData(
    val chapterName: String,
    val duration: Int?,
    val bookName: String?,
    val author: String?
  )
}

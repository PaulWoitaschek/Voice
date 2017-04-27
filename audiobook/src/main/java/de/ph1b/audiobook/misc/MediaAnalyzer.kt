package de.ph1b.audiobook.misc

import android.media.MediaMetadataRetriever
import java.io.File

/**
 * Simple class for analyzing media files and finding information about their metadata.
 *
 * @author Paul Woitaschek
 */
object MediaAnalyzer {

  /**
   * As [MediaMetadataRetriever] has several bugs it is important to catch the exception here as
   * it randomly throws [RuntimeException] on certain implementations.
   */
  private fun MediaMetadataRetriever.safeExtract(key: Int): String? {
    return try {
      extractMetadata(key)
    } catch(ignored: Exception) {
      null
    }
  }

  fun compute(input: File): Result? {
    // Note: MediaMetadataRetriever throws undocumented RuntimeExceptions. We catch these
    // and act appropriate.
    val mmr = MediaMetadataRetriever()
    try {
      mmr.setDataSource(input.absolutePath)

      val parsedDuration: String? = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      val duration = parsedDuration?.toIntOrNull() ?: -1
      if (duration <= 0)
        return null

      // getting chapter-name
      var chapterName = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_TITLE)
      // checking for dot index because otherwise a file called ".mp3" would have no name.
      if (chapterName.isNullOrEmpty()) {
        val fileName = input.nameWithoutExtension
        chapterName = if (fileName.isEmpty()) input.name else fileName
      }

      var author = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ARTIST)
      if (author.isNullOrEmpty())
        author = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)

      val bookName = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUM)

      return Result(duration, chapterName!!, author, bookName)
    } catch(ignored: RuntimeException) {
      return null
    } finally {
      mmr.release()
    }
  }

  data class Result(val duration: Int, val chapterName: String, val author: String?, val bookName: String?) {
    init {
      require(duration > 0)
    }
  }
}


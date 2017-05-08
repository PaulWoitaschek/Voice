package de.ph1b.audiobook.misc

import android.media.MediaMetadataRetriever
import java.io.File
import javax.inject.Inject

/**
 * Extracts meta data from media files. First call prepare.
 *
 * @author Paul Woitaschek
 */
class MetaDataAnalyzer @Inject constructor() {

  private val mmr = MediaMetadataRetriever()
  private var file: File? = null

  // prepare a file and return if that worked
  fun prepare(file: File): Boolean {
    // try preparing twice as MediaMetadataRetriever throws undocumented exceptions
    if (internalPrepare(file))
      return true
    return internalPrepare(file)
  }

  private fun internalPrepare(file: File): Boolean {
    // Note: MediaMetadataRetriever throws undocumented Exceptions. We catch these
    // and act appropriate.
    return try {
      mmr.setDataSource(file.absolutePath)
      this.file = file
      true
    } catch (e: RuntimeException) {
      this.file = null
      false
    }
  }

  fun parseChapterName(): String {
    val file = this.file
        ?: throw IllegalStateException("No prepared file")

    // getting chapter-name
    var chapterName = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_TITLE)
    // checking for dot index because otherwise a file called ".mp3" would have no name.
    if (chapterName.isNullOrEmpty()) {
      val fileName = file.nameWithoutExtension
      chapterName = if (fileName.isEmpty()) file.name else fileName
    }
    return chapterName!!
  }

  fun parseAuthor(): String? {
    check(file != null) { "No prepared file" }

    var author = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ARTIST)
    if (author.isNullOrEmpty())
      author = mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
    return author
  }

  fun parseBookName(): String? {
    check(file != null) { "No prepared file" }

    return mmr.safeExtract(MediaMetadataRetriever.METADATA_KEY_ALBUM)
  }

  /**
   * As [MediaMetadataRetriever] has several bugs it is important to catch the exception here as
   * it randomly throws [RuntimeException] on certain implementations.
   */
  private fun MediaMetadataRetriever.safeExtract(key: Int) = try {
    extractMetadata(key)
  } catch(ignored: Exception) {
    null
  }
}
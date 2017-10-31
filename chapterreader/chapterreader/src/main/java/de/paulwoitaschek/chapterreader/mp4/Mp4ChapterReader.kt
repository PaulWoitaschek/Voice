package de.paulwoitaschek.chapterreader.mp4

import de.paulwoitaschek.chapterreader.misc.Logger
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Reads mp4 chapters
 */
internal class Mp4ChapterReader @Inject constructor(
  private val logger: Logger) {

  fun readChapters(file: File): Map<Int, String> {
    val fromChap: Map<Int, String> = try {
      ChapReader.read(file)
    } catch (e: IOException) {
      logger.e("Error while parsing as chap from $file", e)
      emptyMap()
    }

    if (fromChap.isNotEmpty()) return fromChap

    return try {
      ChplReader.read(file)
    } catch (e: IOException) {
      logger.e("Error while parsing as chpl from $file", e)
      emptyMap()
    }
  }
}

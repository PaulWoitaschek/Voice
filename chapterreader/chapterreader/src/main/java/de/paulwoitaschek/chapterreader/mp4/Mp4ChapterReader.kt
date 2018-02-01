package de.paulwoitaschek.chapterreader.mp4

import de.paulwoitaschek.chapterreader.Chapter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Reads mp4 chapters
 */
internal class Mp4ChapterReader @Inject constructor() {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun readChapters(file: File): List<Chapter> {
    val chapResult = fromChapAtom(file)
    if (chapResult.isNotEmpty()) {
      return chapResult
    }
    return fromChplAtom(file)
  }

  private fun fromChplAtom(file: File): List<Chapter> {
    return try {
      ChplReader.read(file)
    } catch (e: IOException) {
      logger.error("Error while parsing as chpl from $file", e)
      emptyList()
    }
  }

  private fun fromChapAtom(file: File): List<Chapter> {
    return try {
      ChapReader.read(file)
    } catch (e: IOException) {
      logger.error("Error while parsing as chap from $file", e)
      emptyList()
    }
  }
}

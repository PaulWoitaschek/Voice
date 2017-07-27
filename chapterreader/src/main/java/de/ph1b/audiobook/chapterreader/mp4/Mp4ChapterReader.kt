package de.ph1b.audiobook.chapterreader.mp4

import dagger.Reusable
import de.ph1b.audiobook.common.ErrorReporter
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Reads mp4 chapters
 */
@Reusable class Mp4ChapterReader @Inject constructor(private val errorReporter: ErrorReporter) {

  fun readChapters(file: File): Map<Int, String> {
    val fromChap: Map<Int, String> = try {
      ChapReader.read(file)
    } catch (e: IOException) {
      errorReporter.logException(IOException("Error while parsing $file", e))
      emptyMap()
    }

    if (fromChap.isNotEmpty()) return fromChap

    return try {
      ChplReader.read(file)
    } catch (e: IOException) {
      errorReporter.logException(IOException("Error while parsing $file", e))
      emptyMap()
    }
  }
}

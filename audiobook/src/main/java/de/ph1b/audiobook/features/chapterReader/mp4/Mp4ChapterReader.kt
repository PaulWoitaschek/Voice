package de.ph1b.audiobook.features.chapterReader.mp4

import android.util.SparseArray
import de.ph1b.audiobook.features.crashlytics.CrashlyticsProxy
import de.ph1b.audiobook.misc.emptySparseArray
import de.ph1b.audiobook.misc.isNotEmpty
import java.io.File
import java.io.IOException

/**
 * Reads mp4 chapters
 */
object Mp4ChapterReader {

  fun readChapters(file: File): SparseArray<String> {
    val fromChap = try {
      ChapReader.read(file)
    } catch (e: IOException) {
      CrashlyticsProxy.logException(IOException("Error while parsing $file", e))
      emptySparseArray<String>()
    }

    if (fromChap.isNotEmpty()) return fromChap

    return try {
      ChplReader.read(file)
    } catch (e: IOException) {
      CrashlyticsProxy.logException(IOException("Error while parsing $file", e))
      emptySparseArray<String>()
    }
  }
}

package de.paulwoitaschek.chapterreader.matroska

import de.paulwoitaschek.chapterreader.Chapter
import voice.logging.core.Logger
import java.io.File
import java.util.Locale

internal class MatroskaChapterReader(private val readAsMatroskaChapters: ReadAsMatroskaChapters) {

  fun read(file: File): List<Chapter> {
    try {
      val chapters = readAsMatroskaChapters.read(file)
      val preferredLanguages = listOf(Locale.getDefault().isO3Language, "eng")
      return MatroskaChapterFlattener.toChapters(chapters, preferredLanguages)
    } catch (ex: RuntimeException) {
      // JEBML documentation just say's that it throws RuntimeException.
      // For example NullPointerException is thrown if unknown EBML Element
      // type is encountered when calling EBMLReader.readNextElement.
      Logger.w(ex, "Error while reading $file")
    }
    return emptyList()
  }
}

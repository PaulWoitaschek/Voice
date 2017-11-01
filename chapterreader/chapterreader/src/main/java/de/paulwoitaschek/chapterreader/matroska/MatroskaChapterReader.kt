package de.paulwoitaschek.chapterreader.matroska

import de.paulwoitaschek.chapterreader.Chapter
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.RuntimeException
import java.util.Locale
import javax.inject.Inject

internal class MatroskaChapterReader @Inject constructor(
  private val readAsMatroskaChapters: ReadAsMatroskaChapters
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun read(file: File): List<Chapter> {
    try {
      val chapters = readAsMatroskaChapters.read(file)
      val preferredLanguages = listOf(Locale.getDefault().isO3Language, "eng")
      return MatroskaChapterFlattener.toChapters(chapters, preferredLanguages)
    } catch (ex: RuntimeException) {
      // JEBML documentation just say's that it throws RuntimeException.
      // For example NullPointerException is thrown if unknown EBML Element
      // type is encountered when calling EBMLReader.readNextElement.
      logger.error("Error while reading $file", ex)
    }
    return emptyList()
  }
}

package de.ph1b.audiobook.chapterreader.matroska

import dagger.Reusable
import de.ph1b.audiobook.common.Logger
import java.io.File
import java.lang.RuntimeException
import java.util.Locale
import javax.inject.Inject

@Reusable internal class MatroskaChapterReader @Inject constructor(
    private val logger: Logger,
    private val readAsMatroskaChapters: ReadAsMatroskaChapters
) {

  fun read(file: File): Map<Int, String> {
    try {
      val chapters = readAsMatroskaChapters.read(file)
      val preferredLanguages = listOf(Locale.getDefault().isO3Language, "eng")
      return MatroskaChapterFlattener.toMap(chapters, preferredLanguages)
    } catch (ex: RuntimeException) {
      // JEBML documentation just say's that it throws RuntimeException.
      // For example NullPointerException is thrown if unknown EBML Element
      // type is encountered when calling EBMLReader.readNextElement.
      logger.e(ex)
    }
    return emptyMap()
  }
}

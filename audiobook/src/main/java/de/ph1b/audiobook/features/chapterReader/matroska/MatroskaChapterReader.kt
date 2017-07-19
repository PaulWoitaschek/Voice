package de.ph1b.audiobook.features.chapterReader.matroska

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import e
import java.io.File
import java.lang.RuntimeException
import java.util.Locale

object MatroskaChapterReader {

  fun read(file: File): SparseArray<String> {
    try {
      val chapters = ReadAsMatroskaChapters.read(file)
      return MatroskaChapterFlattener.toSparseArray(chapters, Locale.getDefault().isO3Language, "eng")
    } catch (ex: RuntimeException) {
      // JEBML documentation just say's that it throws RuntimeException.
      // For example NullPointerException is thrown if unknown EBML Element
      // type is encountered when calling EBMLReader.readNextElement.
      e(ex)
    }
    return emptySparseArray()
  }
}


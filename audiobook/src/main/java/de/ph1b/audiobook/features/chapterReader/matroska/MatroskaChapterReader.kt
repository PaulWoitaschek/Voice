package de.ph1b.audiobook.features.chapterReader.matroska

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import e
import org.ebml.Element
import org.ebml.ProtoType
import java.io.File
import java.lang.RuntimeException
import java.util.Locale

object MatroskaChapterReader {

  fun read(file: File): SparseArray<String> {
    try {
      val chapters = ReadAsMatroskaChapters.read(file)
      return chapters.flattenToSparseArray(Locale.getDefault().isO3Language, "eng")
    } catch (ex: RuntimeException) {
      // JEBML documentation just say's that it throws RuntimeException.
      // For example NullPointerException is thrown if unknown EBML Element
      // type is encountered when calling EBMLReader.readNextElement.
      e(ex)
    }
    return emptySparseArray()
  }
}

fun List<MatroskaChapter>.flattenToSparseArray(vararg preferredLanguages: String): SparseArray<String> {
  val res = SparseArray<String>()

  fun addChapter(chapters: List<MatroskaChapter>, depth: Int) {
    chapters.forEachIndexed { i, chapter ->
      res.put(
          // Simple hack with adding depth is needed because chapter
          // and it's first sub-chapter have usually the same starting time.
          (chapter.startTime / 1000000).toInt() + if (i == 0) depth else 0,
          "+ ".repeat(depth) + (chapter.getName(*preferredLanguages) ?: "Chapter ${i + 1}"))
      addChapter(chapter.children, depth + 1)
    }
  }

  addChapter(this, 0)

  return res
}


infix fun <T : Element> Element?.isType(t: ProtoType<T>) = this != null && isType(t.type)


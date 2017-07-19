package de.ph1b.audiobook.features.chapterReader.ogg.vorbisComment

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray

data class VorbisComment(val vendor: String, val comments: Map<String, String>) {
  /**
   * Chapters extracted according to https://wiki.xiph.org/Chapter_Extension
   */
  val chapters: SparseArray<String>
    get() {
      val chapters = SparseArray<String>()
      var i = 1
      while (true) {
        val iStr = i.toString().padStart(3, '0')
        val timeStr = comments["CHAPTER$iStr"] ?: break
        val name = comments["CHAPTER${iStr}NAME"] ?: return emptySparseArray()
        val time = VorbisCommentReader.parseChapterTime(timeStr) ?: return emptySparseArray()
        chapters.put(time, name)
        ++i
      }
      return chapters
    }
}

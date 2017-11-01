package de.paulwoitaschek.chapterreader.ogg.vorbisComment

import de.paulwoitaschek.chapterreader.Chapter

internal data class VorbisComment(val vendor: String, val comments: Map<String, String>) {

  /**
   * Chapters extracted according to https://wiki.xiph.org/Chapter_Extension
   */
  fun asChapters(): List<Chapter> {
    val chapters = ArrayList<Chapter>()
    var i = 1
    while (true) {
      val iStr = i.toString().padStart(3, '0')
      val timeStr = comments["CHAPTER$iStr"] ?: break
      val name = comments["CHAPTER${iStr}NAME"] ?: return emptyList()
      val time = VorbisCommentReader.parseChapterTime(timeStr) ?: return emptyList()
      chapters.add(Chapter(time.toLong(), name))
      i++
    }
    return chapters
  }
}

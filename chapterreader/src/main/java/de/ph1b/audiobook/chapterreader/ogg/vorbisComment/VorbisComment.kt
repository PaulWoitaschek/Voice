package de.ph1b.audiobook.chapterreader.ogg.vorbisComment

data class VorbisComment(val vendor: String, val comments: Map<String, String>) {
  /**
   * Chapters extracted according to https://wiki.xiph.org/Chapter_Extension
   */
  val chapters: Map<Int, String>
    get() {
      val chapters = HashMap<Int, String>()
      var i = 1
      while (true) {
        val iStr = i.toString().padStart(3, '0')
        val timeStr = comments["CHAPTER$iStr"] ?: break
        val name = comments["CHAPTER${iStr}NAME"] ?: return emptyMap()
        val time = VorbisCommentReader.parseChapterTime(timeStr) ?: return emptyMap()
        chapters.put(time, name)
        ++i
      }
      return chapters
    }
}

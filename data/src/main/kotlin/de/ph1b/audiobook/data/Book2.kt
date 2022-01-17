package de.ph1b.audiobook.data

data class Book2(
  val content: BookContent2,
  val chapters: List<Chapter2>,
) {

  init {
    if (BuildConfig.DEBUG) {
      val actualPosition = chapters.takeWhile { it.uri != content.uri }
        .sumOf { it.duration } + content.positionInChapter
      check(actualPosition == content.position)
    }
  }

  val currentChapter = chapters[content.currentChapterIndex]
  val previousChapter = chapters.getOrNull(content.currentChapterIndex - 1)
  val nextChapter = chapters.getOrNull(content.currentChapterIndex + 1)

  val nextMark = currentChapter.nextMark(content.positionInChapter)
  val currentMark = currentChapter.markForPosition(content.positionInChapter)

  inline fun update(update: (BookContent2) -> BookContent2): Book2 {
    return copy(content = update(content))
  }
}

private fun Chapter2.nextMark(positionInChapterMs: Long): ChapterMark? {
  val markForPosition = markForPosition(positionInChapterMs)
  val marks = chapterMarks
  val index = marks.indexOf(markForPosition)
  return if (index != -1) {
    marks.getOrNull(index + 1)
  } else {
    null
  }
}

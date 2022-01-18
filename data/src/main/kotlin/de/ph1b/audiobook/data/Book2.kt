package de.ph1b.audiobook.data

import android.net.Uri

data class Book2(
  val content: BookContent2,
  val chapters: List<Chapter2>,
) {

  init {
    if (BuildConfig.DEBUG) {
      val actualPosition = bookPosition(chapters, content.positionInChapter, content.currentChapter)
      check(chapters.size == content.chapters.size) {
        "Different chapter count in $this"
      }
      check(chapters.map { it.uri } == content.chapters) {
        "Different chapter order in $this"
      }
      check(actualPosition == content.position) {
        "Invalid position. expectedPosition=$actualPosition in $this"
      }
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

fun bookPosition(chapters: List<Chapter2>, positionInChapter: Long, currentChapter: Uri): Long {
  require(chapters.isNotEmpty())
  return chapters.takeWhile { it.uri != currentChapter }
    .sumOf { it.duration } + positionInChapter
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

package de.ph1b.audiobook.data

import android.net.Uri

data class Book2(
  val content: BookContent2,
  val chapters: List<Chapter2>,
) {

  val id: Uri = content.uri

  val transitionName: String = id.toString()

  init {
    if (BuildConfig.DEBUG) {
      check(chapters.size == content.chapters.size) {
        "Different chapter count in $this"
      }
      check(chapters.map { it.uri } == content.chapters) {
        "Different chapter order in $this"
      }
    }
  }

  val currentChapter: Chapter2 = chapters[content.currentChapterIndex]
  val previousChapter: Chapter2? = chapters.getOrNull(content.currentChapterIndex - 1)
  val nextChapter: Chapter2? = chapters.getOrNull(content.currentChapterIndex + 1)

  val nextMark: ChapterMark? = currentChapter.nextMark(content.positionInChapter)
  val currentMark: ChapterMark = currentChapter.markForPosition(content.positionInChapter)

  val position: Long = chapters.takeWhile { it.uri != content.currentChapter }
    .sumOf { it.duration } + content.positionInChapter
  val duration: Long = chapters.sumOf { it.duration }

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

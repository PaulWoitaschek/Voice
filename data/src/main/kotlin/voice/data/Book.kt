package voice.data

import android.os.Bundle
import voice.common.BookId

data class Book(
  val content: BookContent,
  val chapters: List<Chapter>,
) {

  val id: BookId = content.id

  init {
    check(chapters.size == content.chapters.size) {
      "Different chapter count in $this"
    }
    check(chapters.map { it.id } == content.chapters) {
      "Different chapter order in $this"
    }
  }

  val currentChapter: Chapter = chapters[content.currentChapterIndex]
  val previousChapter: Chapter? = chapters.getOrNull(content.currentChapterIndex - 1)
  val nextChapter: Chapter? = chapters.getOrNull(content.currentChapterIndex + 1)

  val nextMark: ChapterMark? = currentChapter.nextMark(content.positionInChapter)
  val currentMark: ChapterMark = currentChapter.markForPosition(content.positionInChapter)

  val position: Long = chapters.takeWhile { it.id != content.currentChapter }
    .sumOf { it.duration } + content.positionInChapter
  val duration: Long = chapters.sumOf { it.duration }

  inline fun update(update: (BookContent) -> BookContent): Book {
    return copy(content = update(content))
  }

  companion object {
    const val SPEED_MAX = 2.5F
    const val SPEED_MIN = 0.5F
  }
}

private fun Chapter.nextMark(positionInChapterMs: Long): ChapterMark? {
  val markForPosition = markForPosition(positionInChapterMs)
  val marks = chapterMarks
  val index = marks.indexOf(markForPosition)
  return if (index != -1) {
    marks.getOrNull(index + 1)
  } else {
    null
  }
}

fun Bundle.putBookId(key: String, id: BookId) {
  putString(key, id.value)
}

fun Bundle.getBookId(key: String): BookId? {
  return getString(key)?.let(::BookId)
}

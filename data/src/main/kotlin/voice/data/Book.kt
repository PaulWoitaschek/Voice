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
  val currentMark: ChapterMark = currentChapter.markForPosition(content.positionInChapter)

  val position: Long = chapters.takeWhile { it.id != content.currentChapter }
    .sumOf { it.duration } + content.positionInChapter
  val duration: Long = chapters.sumOf { it.duration }

  inline fun update(update: (BookContent) -> BookContent): Book {
    return copy(content = update(content))
  }
}

fun Bundle.putBookId(
  key: String,
  id: BookId,
) {
  putString(key, id.value)
}

fun Bundle.getBookId(key: String): BookId? {
  return getString(key)?.let(::BookId)
}

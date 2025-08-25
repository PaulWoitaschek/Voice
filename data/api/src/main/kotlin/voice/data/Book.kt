package voice.data

public data class Book(
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

  public inline fun update(update: (BookContent) -> BookContent): Book {
    return copy(content = update(content))
  }
}

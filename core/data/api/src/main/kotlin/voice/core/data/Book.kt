package voice.core.data

public data class Book(
  val content: BookContent,
  val chapters: List<Chapter>,
) {

  val id: BookId = content.id

  init {
    check(chapters.size == content.chapters.size) {
      "The book has ${chapters.size} chapters, but the content lists ${content.chapters.size}"
    }
    check(chapters.map { it.id } == content.chapters) {
      "The chapters are in a different order than in the content"
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

  public fun withElapsedPosition(
    elapsedTime: Long,
    playbackSpeed: Float,
  ): Book {
    val addedAudioTime = (elapsedTime * playbackSpeed).toLong()
    var remainingPositionInChapter = content.positionInChapter + addedAudioTime
    for (index in content.currentChapterIndex until chapters.size) {
      val chapter = chapters[index]
      if (remainingPositionInChapter < chapter.duration || index == chapters.lastIndex) {
        return update {
          it.copy(
            currentChapter = chapter.id,
            positionInChapter = remainingPositionInChapter.coerceAtMost(chapter.duration),
          )
        }
      }
      remainingPositionInChapter -= chapter.duration
    }

    return this
  }
}

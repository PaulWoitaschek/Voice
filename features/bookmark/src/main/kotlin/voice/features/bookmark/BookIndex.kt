package voice.features.bookmark

import voice.core.data.Book
import voice.core.data.ChapterId
import voice.core.data.durationMs
import voice.core.data.markForPosition
import voice.core.ui.formatTime

/**
 * Chapters here are the book's chapter marks, numbered across all files, the same way the player
 * counts them.
 */
internal data class BookLocation(
  val chapterNumber: Int,
  val chapterName: String?,
  val timeInChapter: Long,
  val chapterDuration: Long,
  val bookPosition: Long,
  val bookDuration: Long,
) {

  val time: String get() = formatTime(timeInChapter, chapterDuration)

  val percent: Int
    get() = if (bookDuration > 0) (bookPosition * 100 / bookDuration).toInt().coerceIn(0, 100) else 0

  val progress: Float
    get() = if (bookDuration > 0) (bookPosition.toFloat() / bookDuration).coerceIn(0F, 1F) else 0F
}

internal class BookIndex(private val book: Book) {

  private val chapterIndices = book.chapters.withIndex().associate { (index, chapter) -> chapter.id to index }
  private val chapterOffsets = book.chapters.runningFold(0L) { offset, chapter -> offset + chapter.duration }
  private val firstChapterNumbers = book.chapters.runningFold(1) { number, chapter -> number + chapter.chapterMarks.size }
  private val chapterNames = meaningfulChapterNames(book.chapters.flatMap { chapter -> chapter.chapterMarks.map { it.name } })

  val chapterCount: Int = chapterNames.size

  val duration: Long = book.duration

  val current: BookLocation? = locate(book.content.currentChapter, book.content.positionInChapter)

  /** The chapter segments of the book bar, as fractions of the whole book. */
  val segments: List<Float> = if (duration <= 0L) {
    emptyList()
  } else {
    book.chapters.flatMap { chapter -> chapter.chapterMarks.map { it.durationMs.toFloat() / duration } }
  }

  fun locate(
    chapterId: ChapterId,
    time: Long,
  ): BookLocation? {
    val index = chapterIndices[chapterId] ?: return null
    val chapter = book.chapters[index]
    val clamped = time.coerceIn(0L, chapter.duration.coerceAtLeast(0L))
    val mark = chapter.markForPosition(clamped)
    val number = firstChapterNumbers[index] + chapter.chapterMarks.indexOf(mark).coerceAtLeast(0)
    return BookLocation(
      chapterNumber = number,
      chapterName = chapterNames.getOrNull(number - 1),
      timeInChapter = clamped - mark.startMs,
      chapterDuration = mark.durationMs,
      bookPosition = chapterOffsets[index] + clamped,
      bookDuration = duration,
    )
  }

  fun chapterDuration(chapterId: ChapterId): Long? {
    return chapterIndices[chapterId]?.let { book.chapters[it].duration }
  }
}

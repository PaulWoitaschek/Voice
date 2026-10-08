package voice.features.widget

import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.MarkData
import java.time.Instant
import kotlin.uuid.Uuid

fun book(
  chapters: List<Chapter> = listOf(chapter(), chapter()),
  positionInChapter: Long = 0,
  currentChapter: ChapterId = chapters.first().id,
  speed: Float = 1F,
  lastPlayedAt: Instant = Instant.EPOCH,
  id: BookId = BookId(Uuid.random().toString()),
): Book {
  return Book(
    content = BookContent(
      author = null,
      name = "Moby-Dick",
      positionInChapter = positionInChapter,
      playbackSpeed = speed,
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = currentChapter,
      isActive = true,
      lastPlayedAt = lastPlayedAt,
      skipSilence = false,
      id = id,
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
    ),
    chapters = chapters,
  )
}

fun chapter(
  duration: Long = 10_000,
  marks: List<MarkData> = emptyList(),
  name: String = "Chapter",
): Chapter {
  return Chapter(
    id = ChapterId(Uuid.random().toString()),
    name = name,
    duration = duration,
    fileLastModified = Instant.EPOCH,
    markData = marks,
    fileSize = 0,
  )
}

package voice.features.widget

import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
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
      positionInChapter = positionInChapter,
      playbackSpeed = speed,
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.id },
      currentChapter = currentChapter,
      isActive = true,
      lastPlayedAt = lastPlayedAt,
      skipSilence = false,
      id = id,
      gain = 0F,
      metadata = BookMetadata(
        name = "Moby-Dick",
        author = null,
        album = null,
        title = null,
        genre = null,
        narrator = null,
        series = null,
        part = null,
        cover = null,
        fileLastModified = null,
      ),
      customName = null,
      customCover = null,
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

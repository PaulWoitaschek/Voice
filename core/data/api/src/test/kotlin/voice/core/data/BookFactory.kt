package voice.core.data

import java.time.Instant
import kotlin.uuid.Uuid

fun book(
  name: String = Uuid.random().toString(),
  chapters: List<Chapter> = listOf(chapter(), chapter()),
  time: Long = 42,
  currentChapter: ChapterId = chapters.first().id,
  lastPlayedAtMillis: Long = 0,
  addedAtMillis: Long = 0,
): Book {
  return Book(
    content = BookContent(
      author = Uuid.random().toString(),
      name = name,
      positionInChapter = time,
      playbackSpeed = 1F,
      addedAt = Instant.ofEpochMilli(addedAtMillis),
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = currentChapter,
      isActive = true,
      lastPlayedAt = Instant.ofEpochMilli(lastPlayedAtMillis),
      skipSilence = false,
      id = BookId(Uuid.random().toString()),
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
  duration: Long = 10000,
  id: ChapterId = ChapterId(Uuid.random().toString()),
): Chapter {
  return Chapter(
    id = id,
    name = Uuid.random().toString(),
    duration = duration,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    fileSize = 0,
  )
}

package voice.core.data

import java.time.Instant
import java.util.UUID

fun book(
  name: String = UUID.randomUUID().toString(),
  chapters: List<Chapter> = listOf(chapter(), chapter()),
  time: Long = 42,
  currentChapter: ChapterId = chapters.first().id,
  lastPlayedAtMillis: Long = 0,
  addedAtMillis: Long = 0,
): Book {
  return Book(
    content = BookContent(
      author = UUID.randomUUID().toString(),
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
      id = BookId(UUID.randomUUID().toString()),
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
  id: ChapterId = ChapterId(UUID.randomUUID().toString()),
): Chapter {
  return Chapter(
    id = id,
    name = UUID.randomUUID().toString(),
    duration = duration,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
  )
}

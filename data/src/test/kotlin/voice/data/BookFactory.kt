package voice.data

import java.time.Instant
import java.util.UUID

fun book(
  name: String = UUID.randomUUID().toString(),
  chapters: List<Chapter> = listOf(chapter(), chapter()),
  time: Long = 42,
  currentChapter: Chapter.Id = chapters.first().id,
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
      id = Book.Id(UUID.randomUUID().toString())
    ),
    chapters = chapters,
  )
}

fun chapter(
  duration: Long = 10000,
  id: Chapter.Id = Chapter.Id(UUID.randomUUID().toString())
): Chapter {
  return Chapter(
    id = id,
    name = UUID.randomUUID().toString(),
    duration = duration,
    fileLastModified = Instant.EPOCH,
    markData = emptyList()
  )
}

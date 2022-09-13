package voice.bookOverview

import voice.common.BookId
import voice.data.Book
import voice.data.BookContent
import voice.data.Chapter
import java.time.Instant
import java.util.UUID

fun book(
  chapters: List<Chapter> = listOf(chapter(), chapter()),
  time: Long = 42,
  currentChapter: Chapter.Id = chapters.first().id,
): Book {
  return Book(
    content = BookContent(
      author = UUID.randomUUID().toString(),
      name = UUID.randomUUID().toString(),
      positionInChapter = time,
      playbackSpeed = 1F,
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = currentChapter,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      skipSilence = false,
      id = BookId(UUID.randomUUID().toString()),
      gain = 0F,
    ),
    chapters = chapters,
  )
}

fun chapter(
  duration: Long = 10000,
  id: Chapter.Id = Chapter.Id(UUID.randomUUID().toString()),
): Chapter {
  return Chapter(
    id = id,
    name = UUID.randomUUID().toString(),
    duration = duration,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
  )
}

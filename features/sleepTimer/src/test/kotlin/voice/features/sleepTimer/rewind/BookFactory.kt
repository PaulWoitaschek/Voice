package voice.features.sleepTimer.rewind

import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import java.time.Instant
import kotlin.time.Duration
import kotlin.uuid.Uuid

internal fun book(
  chapterDurations: List<Duration>,
  id: BookId = BookId(Uuid.random().toString()),
): Book {
  val chapters = chapterDurations.map { duration ->
    Chapter(
      id = ChapterId("http://${Uuid.random()}"),
      duration = duration.inWholeMilliseconds,
      fileLastModified = Instant.EPOCH,
      markData = emptyList(),
      name = "chapter",
      fileSize = 0,
    )
  }
  return Book(
    content = BookContent(
      author = "author",
      name = "book",
      positionInChapter = 0,
      playbackSpeed = 1F,
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = chapters.first().id,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
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

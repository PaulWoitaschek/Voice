package voice.core.search

import java.time.Instant
import kotlin.uuid.Uuid
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.CustomMetadata

fun book(
  chapters: List<Chapter> = listOf(chapter(), chapter()),
  time: Long = 42,
  currentChapter: ChapterId = chapters.first().id,
  name: String = Uuid.random().toString(),
  author: String? = Uuid.random().toString(),
  genre: String? = "Genre",
  narrator: String? = "Narrator",
  series: String? = null,
  part: String? = null,
  lastPlayedAt: Instant = Instant.EPOCH,
): Book {
  val id = BookId(Uuid.random().toString())
  return Book(
    content =
      BookContent(
        positionInChapter = time,
        playbackSpeed = 1F,
        addedAt = Instant.EPOCH,
        chapters = chapters.map { it.id },
        currentChapter = currentChapter,
        isActive = true,
        lastPlayedAt = lastPlayedAt,
        skipSilence = false,
        id = id,
        gain = 0F,
        metadata =
          BookMetadata(
            name = name,
            author = author,
            album = null,
            title = null,
            genre = genre,
            narrator = narrator,
            series = series,
            part = part,
            cover = null,
            fileLastModified = null,
          ),
      ),
    chapters = chapters,
    custom = CustomMetadata(id),
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

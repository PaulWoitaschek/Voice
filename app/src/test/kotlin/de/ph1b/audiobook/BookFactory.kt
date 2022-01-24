package de.ph1b.audiobook

import androidx.core.net.toUri
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.Chapter2
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

internal object BookFactory {

  fun create(
    id: UUID = UUID.randomUUID(),
    type: Book.Type = Book.Type.SINGLE_FOLDER,
    author: String = "TestAuthor",
    currentFileIndex: Int = 0,
    time: Long = 0,
    name: String = "TestBook",
    playbackSpeed: Float = 1F,
    loudnessGain: Int = 500,
    skipSilence: Boolean = false,
    chapters: List<Chapter2> = listOf(
      chapter(), chapter(),
    ),
    lastPlayedAtMillis: Long = 0L,
    addedAtMillis: Long = 0L
  ): Book2 {
    return Book2(
      content = BookContent2(
        author = UUID.randomUUID().toString(),
        name = name,
        positionInChapter = 42,
        playbackSpeed = 1F,
        addedAt = Instant.ofEpochMilli(addedAtMillis),
        chapters = chapters.map { it.uri },
        cover = null,
        currentChapter = chapters.first().uri,
        isActive = true,
        lastPlayedAt = Instant.ofEpochMilli(lastPlayedAtMillis),
        skipSilence = false,
        id = Book2.Id(UUID.randomUUID().toString())
      ),
      chapters = chapters,
    )
  }
}

private fun chapter(): Chapter2 {
  return Chapter2(
    uri = "http://${UUID.randomUUID()}".toUri(),
    duration = 5.minutes.inWholeMilliseconds,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    name = "name"
  )
}

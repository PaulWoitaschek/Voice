package de.ph1b.audiobook

import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.Chapter2
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.minutes


fun book(
  name: String = "TestBook",
  lastPlayedAtMillis: Long = 0L,
  addedAtMillis: Long = 0L
): Book2 {
  val chapters = listOf(
    chapter(), chapter(),
  )
  return Book2(
    content = BookContent2(
      author = UUID.randomUUID().toString(),
      name = name,
      positionInChapter = 42,
      playbackSpeed = 1F,
      addedAt = Instant.ofEpochMilli(addedAtMillis),
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = chapters.first().id,
      isActive = true,
      lastPlayedAt = Instant.ofEpochMilli(lastPlayedAtMillis),
      skipSilence = false,
      id = Book2.Id(UUID.randomUUID().toString())
    ),
    chapters = chapters,
  )
}

private fun chapter(): Chapter2 {
  return Chapter2(
    id = Chapter2.Id("http://${UUID.randomUUID()}"),
    duration = 5.minutes.inWholeMilliseconds,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    name = "name"
  )
}

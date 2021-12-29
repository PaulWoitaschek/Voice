package de.ph1b.audiobook

import de.ph1b.audiobook.data.Chapter
import java.io.File
import java.util.UUID

internal object ChapterFactory {

  fun create(
    file: String = "First.mp3",
    parent: String = "/root/",
    duration: Long = 10000,
    lastModified: Long = 12345L,
    bookId: UUID
  ) = Chapter(
    file = File(parent, file),
    name = file,
    duration = duration,
    fileLastModified = lastModified,
    markData = emptyList(),
    bookId = bookId
  )
}

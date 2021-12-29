package de.ph1b.audiobook.data

import java.io.File
import java.util.UUID

internal object ChapterFactory {

  fun create(
    file: String = "First.mp3",
    parent: String = "/root/",
    duration: Long = 100,
    lastModified: Long = 12345L,
    bookId: UUID,
    marks: List<MarkData> = emptyList()
  ) = Chapter(
    file = File(parent, file),
    name = file,
    duration = duration,
    fileLastModified = lastModified,
    markData = marks,
    bookId = bookId
  )
}

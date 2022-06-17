package voice.data.repo

import voice.data.legacy.LegacyChapter
import java.io.File
import java.util.UUID

internal object ChapterFactory {

  fun create(
    file: String = "First.mp3",
    parent: String = "/root/",
    duration: Long = 10000,
    lastModified: Long = 12345L,
    bookId: UUID
  ) = LegacyChapter(
    file = File(parent, file),
    name = file,
    duration = duration,
    fileLastModified = lastModified,
    markData = emptyList(),
    bookId = bookId
  )
}

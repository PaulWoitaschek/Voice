package voice.core.data.repo

import voice.core.data.Chapter
import voice.core.data.ChapterId
import java.time.Instant

public interface ChapterRepo {
  public suspend fun get(id: ChapterId): Chapter?

  public suspend fun put(chapter: Chapter)
}

public suspend inline fun ChapterRepo.getOrPut(
  id: ChapterId,
  lastModified: Instant,
  fileSize: Long,
  fileName: String?,
  defaultValue: () -> Chapter?,
): Chapter? {
  val chapter = get(id)
  if (chapter != null && chapter.fileLastModified == lastModified && chapter.fileSize == fileSize) {
    if (chapter.fileName == fileName) return chapter
    // chapters stored before their file name was get it without analyzing the file again
    return chapter.copy(fileName = fileName).also { put(it) }
  }
  return defaultValue()?.also { put(it) }
}

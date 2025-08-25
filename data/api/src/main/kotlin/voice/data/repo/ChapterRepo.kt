package voice.data.repo

import voice.data.Chapter
import voice.data.ChapterId
import java.time.Instant

public interface ChapterRepo {
  public suspend fun get(id: ChapterId): Chapter?

  public suspend fun put(chapter: Chapter)
}

public suspend inline fun ChapterRepo.getOrPut(
  id: ChapterId,
  lastModified: Instant,
  defaultValue: () -> Chapter?,
): Chapter? {
  val chapter = get(id)
  if (chapter != null && chapter.fileLastModified == lastModified) {
    return chapter
  }
  return defaultValue()?.also { put(it) }
}

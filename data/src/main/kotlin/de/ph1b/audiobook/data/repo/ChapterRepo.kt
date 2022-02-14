package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.internals.dao.ChapterDao
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepo
@Inject constructor(
  private val dao: ChapterDao
) {

  private val cache = mutableMapOf<Chapter.Id, Chapter?>()

  suspend fun get(
    id: Chapter.Id,
    lastModified: Instant? = null
  ): Chapter? {
    if (!cache.containsKey(id)) {
      cache[id] = dao.chapter(id)
    }
    return cache[id]?.takeIf { chapter ->
      lastModified == null || chapter.fileLastModified == lastModified
    }
  }

  suspend fun put(chapter: Chapter) {
    dao.insert(chapter)
    cache[chapter.id] = chapter
  }

  suspend inline fun getOrPut(
    id: Chapter.Id,
    lastModified: Instant,
    defaultValue: () -> Chapter?
  ): Chapter? {
    return get(id, lastModified)
      ?: defaultValue()?.also { put(it) }
  }
}

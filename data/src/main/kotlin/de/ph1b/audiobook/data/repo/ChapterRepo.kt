package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.repo.internals.dao.Chapter2Dao
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepo
@Inject constructor(
  private val dao: Chapter2Dao
) {

  private val cache = mutableMapOf<Chapter2.Id, Chapter2?>()

  suspend fun get(
    id: Chapter2.Id,
    lastModified: Instant? = null
  ): Chapter2? {
    if (!cache.containsKey(id)) {
      cache[id] = dao.chapter(id)
    }
    return cache[id]?.takeIf { chapter ->
      lastModified == null || chapter.fileLastModified == lastModified
    }
  }

  suspend fun put(chapter: Chapter2) {
    dao.insert(chapter)
    cache[chapter.id] = chapter
  }

  suspend inline fun getOrPut(
    id: Chapter2.Id,
    lastModified: Instant,
    defaultValue: () -> Chapter2?
  ): Chapter2? {
    return get(id, lastModified)
      ?: defaultValue()?.also { put(it) }
  }
}

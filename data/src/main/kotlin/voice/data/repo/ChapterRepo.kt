package voice.data.repo

import voice.data.Chapter
import voice.data.ChapterId
import voice.data.repo.internals.dao.ChapterDao
import voice.data.runForMaxSqlVariableNumber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepo
@Inject constructor(private val dao: ChapterDao) {

  private val cache = mutableMapOf<ChapterId, Chapter?>()

  suspend fun get(id: ChapterId): Chapter? {
    // this does not use getOrPut because a `null` value should also be cached
    if (!cache.containsKey(id)) {
      cache[id] = dao.chapter(id)
    }
    return cache[id]
  }

  suspend fun warmup(ids: List<ChapterId>) {
    val missing = ids.filter { it !in cache }
    missing
      .runForMaxSqlVariableNumber {
        dao.chapters(it)
      }
      .forEach { cache[it.id] = it }
  }

  suspend fun put(chapter: Chapter) {
    dao.insert(chapter)
    cache[chapter.id] = chapter
  }

  suspend inline fun getOrPut(
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
}

package voice.data.repo

import dev.zacsweers.metro.Inject
import voice.data.Chapter
import voice.data.ChapterId
import voice.data.repo.internals.dao.ChapterDao
import voice.data.runForMaxSqlVariableNumber
import java.time.Instant

@Inject
public class ChapterRepo
internal constructor(private val dao: ChapterDao) {

  private val cache = mutableMapOf<ChapterId, Chapter?>()

  public suspend fun get(id: ChapterId): Chapter? {
    // this does not use getOrPut because a `null` value should also be cached
    if (!cache.containsKey(id)) {
      cache[id] = dao.chapter(id)
    }
    return cache[id]
  }

  internal suspend fun warmup(ids: List<ChapterId>) {
    val missing = ids.filter { it !in cache }
    missing
      .runForMaxSqlVariableNumber {
        dao.chapters(it)
      }
      .forEach { cache[it.id] = it }
  }

  public suspend fun put(chapter: Chapter) {
    dao.insert(chapter)
    cache[chapter.id] = chapter
  }

  public suspend inline fun getOrPut(
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

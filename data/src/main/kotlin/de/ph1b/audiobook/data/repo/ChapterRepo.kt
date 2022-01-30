package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.repo.internals.dao.Chapter2Dao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepo
@Inject constructor(
  private val dao: Chapter2Dao
) {

  private val cache = mutableMapOf<Chapter2.Id, Chapter2?>()

  private val _chapterChanged = MutableSharedFlow<Chapter2.Id>()
  val chapterChanged: Flow<Chapter2.Id> get() = _chapterChanged

  suspend fun get(id: Chapter2.Id, lastModified: Instant? = null): Chapter2? {
    if (!cache.containsKey(id)) {
      cache[id] = dao.chapter(id).also {
        Timber.d("Chapter for $id wasn't in the map, fetched $it")
      }
    }
    return cache[id]?.takeIf {
      lastModified == null || it.fileLastModified == lastModified
    }
  }

  suspend fun put(chapter: Chapter2) {
    dao.insert(chapter)
    val oldChapter = cache[chapter.id]
    cache[chapter.id] = chapter
    if (oldChapter != chapter) {
      _chapterChanged.emit(chapter.id)
    }
  }

  suspend inline fun getOrPut(id: Chapter2.Id, lastModified: Instant, defaultValue: () -> Chapter2?): Chapter2? {
    return get(id, lastModified)
      ?: defaultValue()?.also { put(it) }
  }
}

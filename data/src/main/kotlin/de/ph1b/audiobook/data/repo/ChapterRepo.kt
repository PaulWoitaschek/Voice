package de.ph1b.audiobook.data.repo

import android.net.Uri
import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.repo.internals.dao.Chapter2Dao
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepo
@Inject constructor(
  private val dao: Chapter2Dao
) {

  private val cache = mutableMapOf<Uri, Chapter2?>()

  suspend fun get(uri: Uri, lastModified: Instant? = null): Chapter2? {
    if (!cache.containsKey(uri)) {
      cache[uri] = dao.chapter(uri).also {
        Timber.d("Chapter for $uri wasn't in the map, fetched $it")
      }
    }
    return cache[uri]?.takeIf {
      lastModified == null || it.fileLastModified == lastModified
    }
  }

  suspend fun put(chapter: Chapter2) {
    dao.insert(chapter)
    cache[chapter.uri] = chapter
  }

  suspend inline fun getOrPut(uri: Uri, lastModified: Instant, defaultValue: () -> Chapter2?): Chapter2? {
    return get(uri, lastModified)
      ?: defaultValue()?.also { put(it) }
  }
}

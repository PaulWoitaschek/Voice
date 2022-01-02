package de.ph1b.audiobook.data.repo

import android.net.Uri
import de.ph1b.audiobook.data.Chapter2
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepo
@Inject constructor() {

  private val cache = mutableMapOf<Uri, Chapter2>()

  fun get(uri: Uri, lastModified: Instant? = null): Chapter2? {
    return cache[uri]?.takeIf {
      lastModified == null || it.fileLastModified == lastModified
    }
  }

  fun put(chapter: Chapter2) {
    cache[chapter.uri] = chapter
  }

  inline fun getOrPut(uri: Uri, lastModified: Instant, defaultValue: () -> Chapter2?): Chapter2? {
    return get(uri, lastModified)
      ?: defaultValue()?.also { put(it) }
  }
}

package de.ph1b.audiobook.data.repo

import android.net.Uri
import de.ph1b.audiobook.data.BookContent2
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookContentRepo
@Inject constructor() {

  private val cache = mutableMapOf<Uri, BookContent2>()

  operator fun get(uri: Uri): BookContent2? {
    return cache[uri]
  }

  fun all(): List<BookContent2> {
    return cache.values.filter { it.isActive }
  }

  fun put(content2: BookContent2) {
    cache[content2.uri] = content2
  }

  inline fun getOrPut(uri: Uri, defaultValue: () -> BookContent2): BookContent2 {
    return get(uri) ?: defaultValue().also { put(it) }
  }

  fun setAllInactiveExcept(uris: List<Uri>) {
    (cache.keys - uris.toSet()).forEach {
      cache[it] = cache.getValue(it).copy(isActive = false)
    }
  }
}

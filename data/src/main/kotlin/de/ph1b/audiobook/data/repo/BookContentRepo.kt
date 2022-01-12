package de.ph1b.audiobook.data.repo

import android.net.Uri
import de.ph1b.audiobook.data.BookContent2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookContentRepo
@Inject constructor() {

  private val cache = MutableStateFlow(mapOf<Uri, BookContent2>())

  operator fun get(uri: Uri): BookContent2? {
    return cache.value[uri]
  }

  fun all(): List<BookContent2> {
    return cache.value.values.filter { it.isActive }
  }

  fun flow(): Flow<List<BookContent2>> = cache.map { it.values.toList().filter { it.isActive } }

  fun put(content2: BookContent2) {
    cache.value = cache.value.toMutableMap().apply {
      this[content2.uri] = content2
    }
  }

  inline fun getOrPut(uri: Uri, defaultValue: () -> BookContent2): BookContent2 {
    return get(uri) ?: defaultValue().also { put(it) }
  }

  fun setAllInactiveExcept(uris: List<Uri>) {
    cache.value = cache.value.toMutableMap().apply {
      (keys - uris.toSet()).forEach {
        this[it] = getValue(it).copy(isActive = false)
      }
    }
  }
}

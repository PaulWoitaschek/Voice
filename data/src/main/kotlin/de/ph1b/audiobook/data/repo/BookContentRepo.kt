package de.ph1b.audiobook.data.repo

import android.net.Uri
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.repo.internals.dao.BookContent2Dao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookContentRepo
@Inject constructor(
  private val dao: BookContent2Dao
) {

  private val cache = MutableStateFlow(mapOf<Uri, BookContent2?>())

  suspend fun get(uri: Uri): BookContent2? {
    val cached = cache.value
    return if (cached.containsKey(uri)) {
      cached[uri]
    } else {
      val content = dao.byId(uri)
      cache.value = cached.toMutableMap().apply {
        put(uri, content)
      }
      content
    }
  }

  fun all(): List<BookContent2> {
    return cache.value.values
      .filterNotNull()
      .filter { it.isActive }
  }

  fun flow(): Flow<List<BookContent2>> {
    return cache.map {
      it.values.toList()
        .filterNotNull()
        .filter { book -> book.isActive }
    }
  }

  suspend fun put(content2: BookContent2) {
    cache.value = cache.value.toMutableMap().apply {
      dao.insert(content2)
      this[content2.uri] = content2
    }
  }

  suspend inline fun getOrPut(uri: Uri, defaultValue: () -> BookContent2): BookContent2 {
    return get(uri) ?: defaultValue().also { put(it) }
  }

  suspend fun setAllInactiveExcept(uris: List<Uri>) {
    cache.value = cache.value.toMutableMap().apply {
      (keys - uris.toSet()).forEach {
        val content = getValue(it) ?: return@forEach
        val updated = content.copy(isActive = false)
        dao.insert(updated)
        this[it] = updated
      }
    }
  }
}

package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.repo.internals.dao.BookContent2Dao
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookContentRepo
@Inject constructor(
  private val dao: BookContent2Dao
) {

  private val cache = MutableStateFlow(mapOf<Book2.Id, BookContent2?>())

  suspend fun get(id: Book2.Id): BookContent2? {
    val cached = cache.value
    return if (cached.containsKey(id)) {
      cached[id]
    } else {
      val content = dao.byId(id)
      cache.value = cached.toMutableMap().apply {
        put(id, content)
      }
      content
    }
  }

  suspend fun put(content2: BookContent2) {
    cache.value = cache.value.toMutableMap().apply {
      dao.insert(content2)
      this[content2.id] = content2
    }
  }

  suspend inline fun getOrPut(id: Book2.Id, defaultValue: () -> BookContent2): BookContent2 {
    return get(id) ?: defaultValue().also { put(it) }
  }
}

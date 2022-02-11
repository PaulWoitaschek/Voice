package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.repo.internals.dao.BookContent2Dao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookContentRepo
@Inject constructor(
  private val dao: BookContent2Dao
) {

  private val cacheMutex = Mutex()
  private var cacheFilled = false
  private val cache = MutableStateFlow<List<BookContent2>?>(null)

  private suspend fun fillCache() {
    if (cacheFilled) {
      return
    }
    cacheMutex.withLock {
      cache.value = dao.all()
      cacheFilled = true
    }
  }

  fun flow(): Flow<List<BookContent2>> {
    return cache.onStart { fillCache() }.filterNotNull()
  }

  fun flow(id: Book2.Id): Flow<BookContent2?> {
    return cache.onStart { fillCache() }
      .filterNotNull()
      .map { contents -> contents.find { it.id == id } }
      .distinctUntilChanged()
  }

  suspend fun get(id: Book2.Id): BookContent2? {
    fillCache()
    return cache.value!!.find { it.id == id }
  }

  suspend fun setAllInactiveExcept(ids: List<Book2.Id>) {
    fillCache()

    cache
      .updateAndGet { contents ->
        contents!!.map { content ->
          content.copy(isActive = content.id in ids)
        }
      }!!
      .also { dao.insert(it) }
  }

  suspend fun put(content: BookContent2) {
    fillCache()
    cache.update { contents ->
      val newContents = contents!!.toMutableList()
      newContents.removeAll { it.id == content.id }
      newContents.add(content)
      dao.insert(content)
      newContents
    }
  }

  suspend inline fun getOrPut(id: Book2.Id, defaultValue: () -> BookContent2): BookContent2 {
    return get(id) ?: defaultValue().also { put(it) }
  }
}

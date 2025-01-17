package voice.data.repo

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
import voice.common.BookId
import voice.data.BookContent
import voice.data.repo.internals.dao.BookContentDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookContentRepo
@Inject constructor(private val dao: BookContentDao) {

  private val cacheMutex = Mutex()
  private var cacheFilled = false
  private val cache = MutableStateFlow<List<BookContent>?>(null)

  private suspend fun fillCache() {
    if (cacheFilled) return
    cacheMutex.withLock {
      if (cacheFilled) return@withLock
      cache.value = dao.all()
      cacheFilled = true
    }
  }

  fun flow(): Flow<List<BookContent>> {
    return cache.onStart { fillCache() }.filterNotNull()
  }

  suspend fun all(): List<BookContent> {
    fillCache()
    return cache.value!!
  }

  fun flow(id: BookId): Flow<BookContent?> {
    return cache.onStart { fillCache() }
      .filterNotNull()
      .map { contents -> contents.find { it.id == id } }
      .distinctUntilChanged()
  }

  suspend fun get(id: BookId): BookContent? {
    fillCache()
    return cache.value!!.find { it.id == id }
  }

  suspend fun setAllInactiveExcept(ids: List<BookId>) {
    fillCache()

    cache
      .updateAndGet { contents ->
        contents!!.map { content ->
          content.copy(isActive = content.id in ids)
        }
      }!!
      .onEach { dao.insert(it) }
  }

  suspend fun put(content: BookContent) {
    fillCache()
    cache.update { contents ->
      val newContents = contents!!.toMutableList()
      newContents.removeAll { it.id == content.id }
      newContents.add(content)
      dao.insert(content)
      newContents
    }
  }

  suspend inline fun getOrPut(
    id: BookId,
    defaultValue: () -> BookContent,
  ): BookContent {
    return get(id) ?: defaultValue().also { put(it) }
  }
}

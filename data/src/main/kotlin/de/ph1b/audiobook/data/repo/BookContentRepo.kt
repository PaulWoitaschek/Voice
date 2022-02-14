package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.repo.internals.dao.BookContentDao
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
  private val dao: BookContentDao
) {

  private val cacheMutex = Mutex()
  private var cacheFilled = false
  private val cache = MutableStateFlow<List<BookContent>?>(null)

  private suspend fun fillCache() {
    if (cacheFilled) {
      return
    }
    cacheMutex.withLock {
      cache.value = dao.all()
      cacheFilled = true
    }
  }

  fun flow(): Flow<List<BookContent>> {
    return cache.onStart { fillCache() }.filterNotNull()
  }

  fun flow(id: Book.Id): Flow<BookContent?> {
    return cache.onStart { fillCache() }
      .filterNotNull()
      .map { contents -> contents.find { it.id == id } }
      .distinctUntilChanged()
  }

  suspend fun get(id: Book.Id): BookContent? {
    fillCache()
    return cache.value!!.find { it.id == id }
  }

  suspend fun setAllInactiveExcept(ids: List<Book.Id>) {
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

  suspend inline fun getOrPut(id: Book.Id, defaultValue: () -> BookContent): BookContent {
    return get(id) ?: defaultValue().also { put(it) }
  }
}

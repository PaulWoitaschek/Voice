package voice.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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
import voice.data.BookContent
import voice.data.BookId
import voice.data.repo.internals.dao.BookContentDao

@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
public class BookContentRepoImpl(private val dao: BookContentDao) : BookContentRepo {

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

  override fun flow(): Flow<List<BookContent>> {
    return cache.onStart { fillCache() }.filterNotNull()
  }

  override suspend fun all(): List<BookContent> {
    fillCache()
    return cache.value!!
  }

  override fun flow(id: BookId): Flow<BookContent?> {
    return cache.onStart { fillCache() }
      .filterNotNull()
      .map { contents -> contents.find { it.id == id } }
      .distinctUntilChanged()
  }

  override suspend fun get(id: BookId): BookContent? {
    fillCache()
    return cache.value!!.find { it.id == id }
  }

  override suspend fun setAllInactiveExcept(ids: List<BookId>) {
    fillCache()

    cache
      .updateAndGet { contents ->
        contents!!.map { content ->
          content.copy(isActive = content.id in ids)
        }
      }!!
      .onEach { dao.insert(it) }
  }

  override suspend fun put(content: BookContent) {
    fillCache()
    cache.update { contents ->
      val newContents = contents!!.toMutableList()
      newContents.removeAll { it.id == content.id }
      newContents.add(content)
      dao.insert(content)
      newContents
    }
  }
}

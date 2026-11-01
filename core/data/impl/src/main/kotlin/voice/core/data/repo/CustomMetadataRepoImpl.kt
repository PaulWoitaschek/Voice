package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.data.BookId
import voice.core.data.CustomMetadata
import voice.core.data.repo.internals.dao.CustomMetadataDao

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
public class CustomMetadataRepoImpl(private val dao: CustomMetadataDao) : CustomMetadataRepo {

  private val cacheMutex = Mutex()
  private var cacheFilled = false
  private val cache = MutableStateFlow<Map<BookId, CustomMetadata>?>(null)

  private suspend fun fillCache() {
    if (cacheFilled) return
    cacheMutex.withLock {
      if (cacheFilled) return@withLock
      cache.value = dao.all().associateBy { it.bookId }
      cacheFilled = true
    }
  }

  override fun flow(): Flow<Map<BookId, CustomMetadata>> {
    return cache.onStart { fillCache() }.filterNotNull()
  }

  override fun flow(id: BookId): Flow<CustomMetadata> {
    return flow().map { it[id] ?: CustomMetadata(id) }.distinctUntilChanged()
  }

  override suspend fun get(id: BookId): CustomMetadata {
    fillCache()
    return cache.value!![id] ?: CustomMetadata(id)
  }

  override suspend fun put(custom: CustomMetadata) {
    fillCache()
    val id = custom.bookId
    cache.update { customs ->
      if (custom == CustomMetadata(id)) {
        dao.delete(id)
        customs!! - id
      } else {
        dao.insert(custom)
        customs!! + (id to custom)
      }
    }
  }
}

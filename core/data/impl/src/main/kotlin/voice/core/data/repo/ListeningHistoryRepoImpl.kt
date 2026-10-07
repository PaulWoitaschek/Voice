package voice.core.data.repo

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.repo.internals.dao.ListeningEventDao
import voice.core.data.store.ListeningHistoryEnabledStore

@ContributesBinding(AppScope::class)
public class ListeningHistoryRepoImpl
internal constructor(
  private val dao: ListeningEventDao,
  @ListeningHistoryEnabledStore
  private val enabledStore: DataStore<Boolean>,
) : ListeningHistoryRepo {

  override fun events(bookId: BookId): Flow<List<ListeningEvent>> = dao.events(bookId)

  override suspend fun add(event: ListeningEvent) {
    if (enabledStore.data.first()) {
      dao.insert(event)
      dao.keepNewest(event.bookId, MAX_EVENTS_PER_BOOK)
    }
  }

  override suspend fun clear() {
    dao.deleteAll()
  }

  internal companion object {
    /**
     * Enough for a few dozen listening sessions, while the table stays small.
     */
    const val MAX_EVENTS_PER_BOOK = 250
  }
}

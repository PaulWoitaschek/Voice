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
import java.time.Clock
import kotlin.time.Duration.Companion.days

@ContributesBinding(AppScope::class)
public class ListeningHistoryRepoImpl
internal constructor(
  private val dao: ListeningEventDao,
  @ListeningHistoryEnabledStore
  private val enabledStore: DataStore<Boolean>,
  private val clock: Clock,
) : ListeningHistoryRepo {

  override fun events(bookId: BookId): Flow<List<ListeningEvent>> = dao.events(bookId)

  override suspend fun add(event: ListeningEvent) {
    if (enabledStore.data.first()) {
      dao.insert(event)
    }
  }

  override suspend fun clear() {
    dao.deleteAll()
  }

  override suspend fun removeExpired() {
    dao.deleteOlderThan(clock.millis() - RETENTION.inWholeMilliseconds)
  }

  internal companion object {
    val RETENTION = 30.days
  }
}

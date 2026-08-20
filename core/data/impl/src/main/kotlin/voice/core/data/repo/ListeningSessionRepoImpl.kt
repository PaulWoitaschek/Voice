package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.repo.internals.dao.ListeningSessionDao
import java.time.Instant

// The repository implementation only forwards queries to the DAO without extra caching or derived state.
@ContributesBinding(AppScope::class)
public class ListeningSessionRepoImpl internal constructor(private val dao: ListeningSessionDao) : ListeningSessionRepo {

  // Keep the repository thin so UI and playback never depend on Room details.
  override suspend fun put(session: ListeningSession) {
    dao.put(session)
  }

  // Only per-book deletion is exposed, so other books' history is never wiped accidentally.
  override suspend fun deleteForBook(bookId: BookId) {
    dao.deleteForBook(bookId)
  }

  // Aggregation bounds are computed by the caller from the local calendar; the data layer only runs the range query.
  override fun durationFlow(
    bookId: BookId,
    startedAt: Instant,
    endedAt: Instant,
  ): Flow<Long> {
    return dao.durationFlow(bookId, startedAt, endedAt)
  }
}

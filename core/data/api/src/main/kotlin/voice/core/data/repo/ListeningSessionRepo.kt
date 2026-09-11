package voice.core.data.repo

import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ListeningSession
import java.time.Instant

// Repository interface isolates playback collection, the Room table, and stats-page query concerns.
public interface ListeningSessionRepo {

  // Playback collection writes the same segment repeatedly; replace lets checkpoints and the final flush share one key.
  public suspend fun put(session: ListeningSession)

  // Per-book entry only clears the current book's stats, keeping global stats out of this change.
  public suspend fun deleteForBook(bookId: BookId)

  // The UI only shows the current period total; the query layer keeps room for library-wide stats later.
  public fun durationFlow(
    bookId: BookId,
    startedAt: Instant,
    endedAt: Instant,
  ): Flow<Long>
}

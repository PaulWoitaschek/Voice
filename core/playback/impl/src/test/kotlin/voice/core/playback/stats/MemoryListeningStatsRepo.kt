package voice.core.playback.stats

import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.ListeningTally
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.repo.ListeningSummary

class MemoryListeningStatsRepo : ListeningStatsRepo {

  val all = mutableListOf<ListeningSession>()
  val tallies = mutableListOf<ListeningTally>()

  override suspend fun save(session: ListeningSession): Long {
    val id = if (session.id == 0L) all.size + 1L else session.id
    val index = all.indexOfFirst { it.id == id }
    if (index == -1) all += session.copy(id = id) else all[index] = session.copy(id = id)
    return id
  }

  override suspend fun summary(
    from: Instant,
    to: Instant,
  ): ListeningSummary = error("Not used")

  override fun sessions(): Flow<List<ListeningSession>> = error("Not used")

  override fun listened(
    from: LocalDate,
    to: LocalDate,
  ): Flow<Duration?> = error("Not used")

  override suspend fun add(tally: ListeningTally) {
    tallies += tally
  }

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  ) = error("Not used")
}

package voice.core.playback.stats

import kotlin.time.Instant
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

  override suspend fun add(tally: ListeningTally) {
    tallies += tally
  }

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  ) = error("Not used")
}

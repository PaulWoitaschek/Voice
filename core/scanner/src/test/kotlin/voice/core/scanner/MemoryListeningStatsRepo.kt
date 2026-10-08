package voice.core.scanner

import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.repo.ListeningSummary
import java.time.Instant

class MemoryListeningStatsRepo : ListeningStatsRepo {

  val all = mutableListOf<ListeningSession>()

  override suspend fun save(session: ListeningSession): Long {
    val id = if (session.id == 0L) all.size + 1L else session.id
    all.removeAll { it.id == id }
    all += session.copy(id = id)
    return id
  }

  override suspend fun summary(
    from: Instant,
    to: Instant,
  ): ListeningSummary = error("Not used by the scanner")

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  ) {
    all.replaceAll { if (it.bookId in from) it.copy(bookId = to) else it }
  }
}

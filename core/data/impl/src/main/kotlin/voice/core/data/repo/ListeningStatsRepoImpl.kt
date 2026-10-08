package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.repo.internals.dao.ListeningSessionDao
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@ContributesBinding(AppScope::class)
public class ListeningStatsRepoImpl
internal constructor(private val dao: ListeningSessionDao) : ListeningStatsRepo {

  override suspend fun save(session: ListeningSession): Long {
    val id = dao.upsert(session)
    // an update returns -1
    return if (id == -1L) session.id else id
  }

  override suspend fun summary(
    from: Instant,
    to: Instant,
  ): ListeningSummary {
    val fromMillis = from.toEpochMilli()
    val toMillis = to.toEpochMilli()
    return ListeningSummary(
      listened = dao.listenedMillis(fromMillis, toMillis).milliseconds,
      listeningDays = dao.listeningDays(fromMillis, toMillis),
      finishedBooks = dao.finishedBooks(fromMillis, toMillis).map {
        FinishedBook(bookId = it.bookId, at = Instant.ofEpochMilli(it.atMillis))
      },
    )
  }

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  ) {
    if (from.isEmpty()) return
    dao.moveToBook(from = from, to = to)
  }
}

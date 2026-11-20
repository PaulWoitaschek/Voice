package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.ListeningTally
import voice.core.data.repo.internals.dao.ListeningSessionDao
import voice.core.data.repo.internals.dao.ListeningTallyDao

@ContributesBinding(AppScope::class)
public class ListeningStatsRepoImpl
internal constructor(
  private val dao: ListeningSessionDao,
  private val tallyDao: ListeningTallyDao,
) : ListeningStatsRepo {

  override suspend fun save(session: ListeningSession): Long {
    val id = dao.upsert(session)
    // an update returns -1
    return if (id == -1L) session.id else id
  }

  override suspend fun summary(
    from: Instant,
    to: Instant,
  ): ListeningSummary {
    val fromMillis = from.toEpochMilliseconds()
    val toMillis = to.toEpochMilliseconds()
    return ListeningSummary(
      listened = dao.listenedMillis(fromMillis, toMillis).milliseconds,
      listeningDays = dao.listeningDays(fromMillis, toMillis),
      finishedBooks =
        dao.finishedBooks(fromMillis, toMillis).map {
          FinishedBook(bookId = it.bookId, at = Instant.fromEpochMilliseconds(it.atMillis))
        },
    )
  }

  override fun sessions(): Flow<List<ListeningSession>> = dao.all()

  override fun listened(
    from: LocalDate,
    to: LocalDate,
  ): Flow<Duration?> =
    dao.listenedMillisOnDays(from.toEpochDay(), to.toEpochDay()).map { it?.milliseconds }

  override suspend fun add(tally: ListeningTally) {
    tallyDao.add(tally.day, tally.bookId, tally.name, tally.number)
  }

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  ) {
    if (from.isEmpty()) return
    dao.moveToBook(from = from, to = to)
    tallyDao.moveToBook(from = from, to = to)
  }
}

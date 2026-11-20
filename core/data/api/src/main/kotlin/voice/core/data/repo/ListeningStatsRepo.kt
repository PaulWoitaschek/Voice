package voice.core.data.repo

import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.ListeningTally

/** Listening sessions, kept on the device to tell listeners about their listening. */
public interface ListeningStatsRepo {

  /** Stores a new session, or updates it if it has an id. Returns the id. */
  public suspend fun save(session: ListeningSession): Long

  /** Sums up the sessions that started in between [from] (inclusive) and [to] (exclusive). */
  public suspend fun summary(
    from: Instant = Instant.DISTANT_PAST,
    to: Instant = Instant.DISTANT_FUTURE,
  ): ListeningSummary

  public fun sessions(): Flow<List<ListeningSession>>

  // on the local days from [from] until [to], or null while Voice never counted any listening
  public fun listened(
    from: LocalDate,
    to: LocalDate,
  ): Flow<Duration?>

  public suspend fun add(tally: ListeningTally)

  /** Hands the sessions over when a book shows up under a new id. */
  public suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  )
}

public data class ListeningSummary(
  val listened: Duration,
  /** The number of local days with listening. */
  val listeningDays: Int,
  /** Newest first, each book once. */
  val finishedBooks: List<FinishedBook>,
)

public data class FinishedBook(
  val bookId: BookId,
  val at: Instant,
)

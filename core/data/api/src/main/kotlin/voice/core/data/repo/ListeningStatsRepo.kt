package voice.core.data.repo

import voice.core.data.BookId
import voice.core.data.ListeningSession
import java.time.Instant
import kotlin.time.Duration

/**
 * Listening sessions, kept on the device to tell listeners about their listening.
 */
public interface ListeningStatsRepo {

  /**
   * Stores a new session, or updates it if it has an id. Returns the id.
   */
  public suspend fun save(session: ListeningSession): Long

  /**
   * Sums up the sessions that started in between [from] (inclusive) and [to] (exclusive).
   */
  public suspend fun summary(
    from: Instant = Instant.EPOCH,
    to: Instant = Instant.ofEpochMilli(Long.MAX_VALUE),
  ): ListeningSummary

  /**
   * Hands the sessions over when a book shows up under a new id.
   */
  public suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  )
}

public data class ListeningSummary(
  val listened: Duration,
  /**
   * The number of local days with listening.
   */
  val listeningDays: Int,
  /**
   * Newest first, each book once.
   */
  val finishedBooks: List<FinishedBook>,
)

public data class FinishedBook(
  val bookId: BookId,
  val at: Instant,
)

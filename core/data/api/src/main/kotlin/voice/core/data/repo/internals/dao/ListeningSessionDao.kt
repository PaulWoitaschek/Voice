package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ListeningSession
import java.time.Instant

// DAO only offers write, per-book cleanup, and period aggregation to keep the stats table surface minimal.
@Dao
public interface ListeningSessionDao {

  // Segment keys are owned by the collector, so replace lets an open checkpoint be updated to its final duration.
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public suspend fun put(session: ListeningSession)

  // Deletion is per book, matching the long-press book menu entry.
  @Query("DELETE FROM listening_session WHERE bookId = :bookId")
  public suspend fun deleteForBook(bookId: BookId)

  // Sessions are split at local day boundaries during collection, so period queries can aggregate by start time.
  @Query(
    """
    SELECT COALESCE(SUM(durationMs), 0)
    FROM listening_session
    WHERE bookId = :bookId
    AND startedAt >= :startedAt
    AND startedAt < :endedAt
    """,
  )
  public fun durationFlow(
    bookId: BookId,
    startedAt: Instant,
    endedAt: Instant,
  ): Flow<Long>
}

package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ListeningEvent

@Dao
public interface ListeningEventDao {

  @Insert
  public suspend fun insert(event: ListeningEvent)

  @Query("SELECT * FROM listening_event WHERE bookId = :bookId ORDER BY atMillis DESC, id DESC")
  public fun events(bookId: BookId): Flow<List<ListeningEvent>>

  @Query(
    """
    DELETE FROM listening_event WHERE bookId = :bookId AND id NOT IN (
      SELECT id FROM listening_event WHERE bookId = :bookId ORDER BY atMillis DESC, id DESC LIMIT :count
    )
    """,
  )
  public suspend fun keepNewest(
    bookId: BookId,
    count: Int,
  )

  @Query("DELETE FROM listening_event")
  public suspend fun deleteAll()
}

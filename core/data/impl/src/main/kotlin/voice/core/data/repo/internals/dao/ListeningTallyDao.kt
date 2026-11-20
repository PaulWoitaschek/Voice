package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import java.time.LocalDate
import voice.core.data.BookId
import voice.core.data.ListeningTally

@Dao
public abstract class ListeningTallyDao {

  @Query(
    """
    INSERT OR REPLACE INTO listening_tally (day, bookId, name, number)
    VALUES (:day, :bookId, :name, :number + COALESCE(
      (SELECT number FROM listening_tally WHERE day = :day AND bookId = :bookId AND name = :name), 0))
    """,
  )
  public abstract suspend fun add(
    day: LocalDate,
    bookId: BookId,
    name: String,
    number: Long,
  )

  @Query("SELECT * FROM listening_tally WHERE bookId IN (:bookIds)")
  public abstract suspend fun ofBooks(
    bookIds: List<@JvmSuppressWildcards BookId>,
  ): List<ListeningTally>

  @Query("DELETE FROM listening_tally WHERE bookId IN (:bookIds)")
  public abstract suspend fun delete(bookIds: List<@JvmSuppressWildcards BookId>)

  @Transaction
  public open suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  ) {
    val moved = ofBooks(from)
    delete(from)
    moved.forEach { add(it.day, to, it.name, it.number) }
  }
}

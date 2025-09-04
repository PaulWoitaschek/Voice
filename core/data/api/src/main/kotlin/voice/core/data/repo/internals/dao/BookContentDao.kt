package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions.TOKENIZER_UNICODE61
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import voice.core.data.BookContent
import voice.core.data.BookId

@Dao
public interface BookContentDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public suspend fun insert(content: BookContent)

  @Query("SELECT * FROM content2")
  public suspend fun all(): List<BookContent>

  @Query(
    """
  SELECT id
  FROM bookSearchFts
  WHERE bookSearchFts MATCH :query
  AND isActive = 1
    """,
  )
  public suspend fun search(query: String): List<BookId>
}

@Entity(tableName = "bookSearchFts")
@Fts4(
  contentEntity = BookContent::class,
  tokenizer = TOKENIZER_UNICODE61,
  notIndexed = ["id", "isActive"],
)
public data class BookSearchFts(
  val name: String,
  val author: String?,
  val genre: String?,
  val narrator: String?,
  val series: String?,
  val part: String?,
  val id: BookId,
  val isActive: Boolean,
)

package voice.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions.TOKENIZER_UNICODE61
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import voice.common.BookId
import voice.data.BookContent

@Dao
interface BookContentDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(content: BookContent)

  @Query("SELECT * FROM content2")
  suspend fun all(): List<BookContent>

  @Query(
    """
  SELECT id
  FROM bookSearchFts
  WHERE bookSearchFts MATCH :query
  AND isActive = 1
    """,
  )
  suspend fun search(query: String): List<BookId>
}

@Entity(tableName = "bookSearchFts")
@Fts4(
  contentEntity = BookContent::class,
  tokenizer = TOKENIZER_UNICODE61,
  notIndexed = ["id", "isActive"],
)
data class BookSearchFts(
  val name: String,
  val author: String?,
  val id: BookId,
  val isActive: Boolean,
)

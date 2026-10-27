package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import voice.core.data.BookContent

@Dao
public interface BookContentDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public suspend fun insert(content: BookContent)

  @Query("SELECT * FROM content2")
  public suspend fun all(): List<BookContent>
}

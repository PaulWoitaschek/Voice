package voice.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import voice.data.BookContent

@Dao
interface BookContentDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(content: BookContent)

  @Query("SELECT * FROM content2")
  suspend fun all(): List<BookContent>
}

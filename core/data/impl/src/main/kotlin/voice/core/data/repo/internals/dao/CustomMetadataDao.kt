package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import voice.core.data.BookId
import voice.core.data.CustomMetadata

@Dao
public interface CustomMetadataDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE) public suspend fun insert(custom: CustomMetadata)

  @Query("DELETE FROM customMetadata WHERE bookId = :id") public suspend fun delete(id: BookId)

  @Query("SELECT * FROM customMetadata") public suspend fun all(): List<CustomMetadata>
}

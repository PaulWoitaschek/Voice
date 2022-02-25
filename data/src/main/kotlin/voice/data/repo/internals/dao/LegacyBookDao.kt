package voice.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface LegacyBookDao {

  @Query("SELECT COUNT(*) from bookMetaData")
  suspend fun bookMetaDataCount(): Int
}

package voice.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query
import voice.data.legacy.LegacyBookMetaData
import voice.data.legacy.LegacyBookSettings
import voice.data.legacy.LegacyBookmark
import voice.data.legacy.LegacyChapter

@Dao
interface LegacyBookDao {

  @Query("SELECT COUNT(*) from bookMetaData")
  suspend fun bookMetaDataCount(): Int

  @Query("SELECT * FROM bookMetaData")
  suspend fun bookMetaData(): List<LegacyBookMetaData>

  @Query("SELECT * FROM bookSettings")
  suspend fun settings(): List<LegacyBookSettings>

  @Query("SELECT * FROM chapters")
  suspend fun chapters(): List<LegacyChapter>

  @Query("SELECT * FROM bookmark")
  suspend fun bookmarks(): List<LegacyBookmark>
}

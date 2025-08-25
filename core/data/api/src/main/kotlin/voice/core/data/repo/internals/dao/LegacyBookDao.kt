package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query
import voice.core.data.legacy.LegacyBookMetaData
import voice.core.data.legacy.LegacyBookSettings
import voice.core.data.legacy.LegacyBookmark
import voice.core.data.legacy.LegacyChapter
import java.io.File
import java.util.UUID

@Dao
public interface LegacyBookDao {

  @Query("SELECT COUNT(*) from bookMetaData")
  public suspend fun bookMetaDataCount(): Int

  @Query("SELECT * FROM bookMetaData")
  public suspend fun bookMetaData(): List<LegacyBookMetaData>

  @Query("SELECT * FROM bookSettings")
  public suspend fun settings(): List<LegacyBookSettings>

  @Query("SELECT * FROM bookSettings WHERE id = :id")
  public suspend fun settingsById(id: UUID): LegacyBookSettings?

  @Query("SELECT * FROM chapters")
  public suspend fun chapters(): List<LegacyChapter>

  @Query("SELECT * FROM bookmark")
  public suspend fun bookmarks(): List<LegacyBookmark>

  @Query("SELECT * FROM bookmark WHERE file IN(:chapters)")
  public suspend fun bookmarksByFiles(chapters: List<@JvmSuppressWildcards File>): List<LegacyBookmark>

  @Query("DELETE FROM bookmark")
  public suspend fun deleteBookmarks()

  @Query("DELETE FROM chapters")
  public suspend fun deleteChapters()

  @Query("DELETE FROM bookSettings")
  public suspend fun deleteSettings()

  @Query("DELETE FROM bookMetaData")
  public suspend fun deleteBookMetaData()
  public suspend fun deleteAll() {
    deleteBookMetaData()
    deleteSettings()
    deleteChapters()
    deleteBookmarks()
  }
}

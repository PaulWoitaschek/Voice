package voice.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query
import voice.data.legacy.LegacyBookMetaData
import voice.data.legacy.LegacyBookSettings
import voice.data.legacy.LegacyBookmark
import voice.data.legacy.LegacyChapter
import java.io.File
import java.util.UUID

@Dao
interface LegacyBookDao {

  @Query("SELECT COUNT(*) from bookMetaData")
  suspend fun bookMetaDataCount(): Int

  @Query("SELECT * FROM bookMetaData")
  suspend fun bookMetaData(): List<LegacyBookMetaData>

  @Query("SELECT * FROM bookSettings")
  suspend fun settings(): List<LegacyBookSettings>

  @Query("SELECT * FROM bookSettings WHERE id = :id")
  suspend fun settingsById(id: UUID): LegacyBookSettings?

  @Query("SELECT * FROM chapters")
  suspend fun chapters(): List<LegacyChapter>

  @Query("SELECT * FROM bookmark")
  suspend fun bookmarks(): List<LegacyBookmark>

  @Query("SELECT * FROM bookmark WHERE file IN(:chapters)")
  suspend fun bookmarksByFiles(chapters: List<@JvmSuppressWildcards File>): List<LegacyBookmark>

  @Query("DELETE FROM bookmark")
  suspend fun deleteBookmarks()

  @Query("DELETE FROM chapters")
  suspend fun deleteChapters()

  @Query("DELETE FROM bookSettings")
  suspend fun deleteSettings()

  @Query("DELETE FROM bookMetaData")
  suspend fun deleteBookMetaData()
  suspend fun deleteAll() {
    deleteBookMetaData()
    deleteSettings()
    deleteChapters()
    deleteBookmarks()
  }
}

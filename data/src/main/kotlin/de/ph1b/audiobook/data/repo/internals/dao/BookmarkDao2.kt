package de.ph1b.audiobook.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.ph1b.audiobook.data.Bookmark2
import de.ph1b.audiobook.data.Chapter2

@Dao
interface BookmarkDao2 {

  @Query("DELETE FROM bookmark2 WHERE id = :id")
  suspend fun deleteBookmark(id: Bookmark2.Id)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addBookmark(bookmark: Bookmark2)

  @Query("SELECT * FROM bookmark2 WHERE chapterId IN(:chapters)")
  suspend fun allForFiles(chapters: List<@JvmSuppressWildcards Chapter2.Id>): List<Bookmark2>
}

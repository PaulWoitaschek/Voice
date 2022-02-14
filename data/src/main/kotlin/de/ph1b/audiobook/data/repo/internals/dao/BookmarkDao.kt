package de.ph1b.audiobook.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Chapter

@Dao
interface BookmarkDao {

  @Query("DELETE FROM bookmark2 WHERE id = :id")
  suspend fun deleteBookmark(id: Bookmark.Id)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addBookmark(bookmark: Bookmark)

  @Query("SELECT * FROM bookmark2 WHERE chapterId IN(:chapters)")
  suspend fun allForChapters(chapters: List<@JvmSuppressWildcards Chapter.Id>): List<Bookmark>
}

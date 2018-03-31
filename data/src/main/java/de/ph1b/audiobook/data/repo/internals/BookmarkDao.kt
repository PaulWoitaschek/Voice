package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import de.ph1b.audiobook.data.Bookmark
import java.io.File

@Dao
interface BookmarkDao {

  @Query("DELETE FROM bookmark WHERE id = :id")
  fun deleteBookmark(id: Long)

  @Insert
  fun addBookmark(bookmark: Bookmark): Long

  @Query("SELECT * FROM bookmark WHERE file IN(:files)")
  fun allForFiles(files: List<@JvmSuppressWildcards File>): List<Bookmark>
}

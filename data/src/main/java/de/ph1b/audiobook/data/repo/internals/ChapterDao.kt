package de.ph1b.audiobook.data.repo.internals

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.ph1b.audiobook.data.Chapter
import java.util.UUID

@Dao
interface ChapterDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(chapters: List<Chapter>)

  @Query("SELECT * FROM chapters WHERE bookId = :bookId")
  fun byBookId(bookId: UUID): List<Chapter>

  @Query("DELETE FROM chapters WHERE bookId = :bookId")
  fun deleteByBookId(bookId: UUID)
}

package de.ph1b.audiobook.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2

@Dao
interface BookContent2Dao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(content2: BookContent2)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(content2: List<BookContent2>)

  @Query("SELECT * FROM content2 WHERE id = :id")
  suspend fun byId(id: Book2.Id): BookContent2?

  @Query("SELECT * FROM content2 WHERE isActive = :isActive")
  suspend fun all(isActive: Boolean): List<BookContent2>
}

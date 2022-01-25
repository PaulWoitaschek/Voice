package de.ph1b.audiobook.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.ph1b.audiobook.data.Chapter2

@Dao
interface Chapter2Dao {

  @Query("SELECT * FROM chapters2 WHERE id = :id")
  suspend fun chapter(id: Chapter2.Id): Chapter2?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(chapter2: Chapter2)
}

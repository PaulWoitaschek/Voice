package de.ph1b.audiobook.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.ph1b.audiobook.data.BookMetaData
import java.util.UUID

@Dao
interface BookMetaDataDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(metaData: BookMetaData)

  @Query("SELECT * FROM bookMetaData WHERE id = :id")
  suspend fun byId(id: UUID): BookMetaData

  @Delete
  suspend fun delete(metaData: BookMetaData)

  @Query("UPDATE bookMetaData SET name = :name WHERE id = :id")
  suspend fun updateBookName(id: UUID, name: String)
}

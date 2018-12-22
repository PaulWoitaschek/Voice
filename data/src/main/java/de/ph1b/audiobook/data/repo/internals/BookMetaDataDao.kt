package de.ph1b.audiobook.data.repo.internals

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
  fun insert(metaData: BookMetaData)

  @Query("SELECT * FROM bookMetaData WHERE id = :id")
  fun byId(id: UUID): BookMetaData

  @Delete
  fun delete(metaData: BookMetaData)
}

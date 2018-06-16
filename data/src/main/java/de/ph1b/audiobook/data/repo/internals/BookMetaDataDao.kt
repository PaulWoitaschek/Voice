package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import de.ph1b.audiobook.data.BookMetaData
import java.util.UUID

@Dao
interface BookMetaDataDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(metaData: BookMetaData)

  @Query("SELECT * FROM bookMetaData WHERE id = :id")
  fun byId(id: UUID): BookMetaData
}

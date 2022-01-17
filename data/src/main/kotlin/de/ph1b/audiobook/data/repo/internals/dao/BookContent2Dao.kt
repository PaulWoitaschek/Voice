package de.ph1b.audiobook.data.repo.internals.dao

import android.net.Uri
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.ph1b.audiobook.data.BookContent2

@Dao
interface BookContent2Dao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(content2: BookContent2)

  @Query("SELECT * FROM content2 WHERE uri = :id")
  suspend fun byId(id: Uri): BookContent2?
}

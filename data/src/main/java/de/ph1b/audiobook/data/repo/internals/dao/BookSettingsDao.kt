package de.ph1b.audiobook.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.ph1b.audiobook.data.BookSettings
import java.util.UUID

@Dao
interface BookSettingsDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(bookSettings: BookSettings)

  @Query("SELECT * FROM bookSettings")
  suspend fun all(): List<BookSettings>

  @Query("UPDATE bookSettings SET active=:active WHERE id=:id")
  suspend fun setActive(id: UUID, active: Boolean)

  @Delete
  suspend fun delete(bookSettings: BookSettings)

  @Query("UPDATE bookSettings SET lastPlayedAtMillis = :lastPlayedAtMillis WHERE id = :id")
  suspend fun updateLastPlayedAt(id: UUID, lastPlayedAtMillis: Long)
}

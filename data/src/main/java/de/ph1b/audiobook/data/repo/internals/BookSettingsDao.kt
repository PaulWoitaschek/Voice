package de.ph1b.audiobook.data.repo.internals

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
  fun insert(bookSettings: BookSettings)

  @Query("SELECT * FROM bookSettings")
  fun all(): List<BookSettings>

  @Query("UPDATE bookSettings SET active=:active WHERE id=:id")
  fun setActive(id: UUID, active: Boolean)

  @Delete
  fun delete(bookSettings: BookSettings)

  @Query("UPDATE bookSettings SET lastPlayedAtMillis = :lastPlayedAtMillis WHERE id = :id")
  fun updateLastPlayedAt(id: UUID, lastPlayedAtMillis: Long)
}

package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
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
}

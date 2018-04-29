package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import de.ph1b.audiobook.data.Bookmark

@Database(
  entities = [Bookmark::class],
  version = AppDb.VERSION
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {

  abstract fun bookmarkDao(): BookmarkDao

  companion object {
    const val VERSION = 44
    const val DATABASE_NAME = "autoBookDB"
  }
}

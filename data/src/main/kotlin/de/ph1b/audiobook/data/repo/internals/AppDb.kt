package de.ph1b.audiobook.data.repo.internals

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.BookMetaData
import de.ph1b.audiobook.data.BookSettings
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Bookmark2
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.repo.internals.dao.BookContent2Dao
import de.ph1b.audiobook.data.repo.internals.dao.BookMetaDataDao
import de.ph1b.audiobook.data.repo.internals.dao.BookSettingsDao
import de.ph1b.audiobook.data.repo.internals.dao.BookmarkDao
import de.ph1b.audiobook.data.repo.internals.dao.BookmarkDao2
import de.ph1b.audiobook.data.repo.internals.dao.Chapter2Dao
import de.ph1b.audiobook.data.repo.internals.dao.ChapterDao

@Database(
  entities = [
    Bookmark::class,
    Chapter::class,
    BookMetaData::class,
    BookSettings::class,
    Chapter2::class,
    BookContent2::class,
    Bookmark2::class,
  ],
  version = AppDb.VERSION,
  autoMigrations = [AutoMigration(from = 51, to = 52)]
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {

  abstract fun bookmarkDao(): BookmarkDao
  abstract fun chapterDao(): ChapterDao
  abstract fun bookMetadataDao(): BookMetaDataDao
  abstract fun bookSettingsDao(): BookSettingsDao
  abstract fun chapter2Dao(): Chapter2Dao
  abstract fun bookContent2Dao(): BookContent2Dao
  abstract fun bookmarkDao2(): BookmarkDao2

  companion object {
    const val VERSION = 52
    const val DATABASE_NAME = "autoBookDB"
  }
}

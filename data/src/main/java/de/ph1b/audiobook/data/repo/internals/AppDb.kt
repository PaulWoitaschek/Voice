package de.ph1b.audiobook.data.repo.internals

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.ph1b.audiobook.data.BookMetaData
import de.ph1b.audiobook.data.BookSettings
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Chapter

@Database(
  entities = [Bookmark::class, Chapter::class, BookMetaData::class, BookSettings::class],
  version = AppDb.VERSION
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {

  abstract fun bookmarkDao(): BookmarkDao
  abstract fun chapterDao(): ChapterDao
  abstract fun bookMetadataDao(): BookMetaDataDao
  abstract fun bookSettingsDao(): BookSettingsDao

  companion object {
    const val VERSION = 48
    const val DATABASE_NAME = "autoBookDB"
  }
}

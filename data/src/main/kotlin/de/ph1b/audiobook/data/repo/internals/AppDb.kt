package de.ph1b.audiobook.data.repo.internals

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.LegacyChapter
import de.ph1b.audiobook.data.legacy.LegacyBookMetaData
import de.ph1b.audiobook.data.legacy.LegacyBookSettings
import de.ph1b.audiobook.data.legacy.LegacyBookmark
import de.ph1b.audiobook.data.repo.internals.dao.BookContentDao
import de.ph1b.audiobook.data.repo.internals.dao.BookmarkDao
import de.ph1b.audiobook.data.repo.internals.dao.ChapterDao

@Database(
  entities = [
    LegacyBookmark::class,
    LegacyChapter::class,
    LegacyBookMetaData::class,
    LegacyBookSettings::class,
    Chapter::class,
    BookContent::class,
    Bookmark::class,
  ],
  version = AppDb.VERSION,
  autoMigrations = [AutoMigration(from = 51, to = 52)]
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {

  abstract fun chapterDao(): ChapterDao
  abstract fun bookContentDao(): BookContentDao
  abstract fun bookmarkDao(): BookmarkDao

  companion object {
    const val VERSION = 52
    const val DATABASE_NAME = "autoBookDB"
  }
}

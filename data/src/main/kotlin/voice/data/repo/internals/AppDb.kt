package voice.data.repo.internals

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import voice.data.BookContent
import voice.data.Bookmark
import voice.data.Chapter
import voice.data.RecentBookSearch
import voice.data.legacy.LegacyBookMetaData
import voice.data.legacy.LegacyBookSettings
import voice.data.legacy.LegacyBookmark
import voice.data.legacy.LegacyChapter
import voice.data.repo.internals.dao.BookContentDao
import voice.data.repo.internals.dao.BookSearchFts
import voice.data.repo.internals.dao.BookmarkDao
import voice.data.repo.internals.dao.ChapterDao
import voice.data.repo.internals.dao.LegacyBookDao
import voice.data.repo.internals.dao.RecentBookSearchDao

@Database(
  entities = [
    LegacyBookmark::class,
    LegacyChapter::class,
    LegacyBookMetaData::class,
    LegacyBookSettings::class,
    Chapter::class,
    BookContent::class,
    Bookmark::class,
    BookSearchFts::class,
    RecentBookSearch::class,
  ],
  version = AppDb.VERSION,
  autoMigrations = [
    AutoMigration(from = 51, to = 52),
    AutoMigration(from = 52, to = 53),
    AutoMigration(from = 54, to = 55),
    AutoMigration(from = 55, to = 56),
  ],
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {

  abstract fun chapterDao(): ChapterDao
  abstract fun bookContentDao(): BookContentDao
  abstract fun bookmarkDao(): BookmarkDao
  abstract fun legacyBookDao(): LegacyBookDao
  abstract fun recentBookSearchDao(): RecentBookSearchDao

  companion object {
    const val VERSION = 56
    const val DATABASE_NAME = "autoBookDB"
  }
}

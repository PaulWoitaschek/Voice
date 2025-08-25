package voice.core.data.repo.internals

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.RecentBookSearch
import voice.core.data.legacy.LegacyBookMetaData
import voice.core.data.legacy.LegacyBookSettings
import voice.core.data.legacy.LegacyBookmark
import voice.core.data.legacy.LegacyChapter
import voice.core.data.repo.internals.dao.BookContentDao
import voice.core.data.repo.internals.dao.BookSearchFts
import voice.core.data.repo.internals.dao.BookmarkDao
import voice.core.data.repo.internals.dao.ChapterDao
import voice.core.data.repo.internals.dao.LegacyBookDao
import voice.core.data.repo.internals.dao.RecentBookSearchDao

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
public abstract class AppDb : RoomDatabase() {

  public abstract fun chapterDao(): ChapterDao
  public abstract fun bookContentDao(): BookContentDao
  public abstract fun bookmarkDao(): BookmarkDao
  public abstract fun legacyBookDao(): LegacyBookDao
  public abstract fun recentBookSearchDao(): RecentBookSearchDao

  internal companion object {
    const val VERSION = 56
    const val DATABASE_NAME = "autoBookDB"
  }
}

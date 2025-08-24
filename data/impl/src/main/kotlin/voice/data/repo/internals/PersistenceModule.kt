package voice.data.repo.internals

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import voice.data.repo.internals.dao.BookContentDao
import voice.data.repo.internals.dao.BookmarkDao
import voice.data.repo.internals.dao.ChapterDao
import voice.data.repo.internals.dao.LegacyBookDao
import voice.data.repo.internals.dao.RecentBookSearchDao

@BindingContainer
@ContributesTo(AppScope::class)
internal object PersistenceModule {

  @Provides
  fun chapterDao(appDb: AppDb): ChapterDao = appDb.chapterDao()

  @Provides
  fun bookContentDao(appDb: AppDb): BookContentDao = appDb.bookContentDao()

  @Provides
  fun bookmarkDao(appDb: AppDb): BookmarkDao = appDb.bookmarkDao()

  @Provides
  fun legacyBookDao(appDb: AppDb): LegacyBookDao = appDb.legacyBookDao()

  @Provides
  fun recentBookSearchDao(appDb: AppDb): RecentBookSearchDao = appDb.recentBookSearchDao()

  @Provides
  @SingleIn(AppScope::class)
  fun appDb(
    context: Context,
    migrations: Set<@JvmSuppressWildcards Migration>,
  ): AppDb {
    return Room.databaseBuilder(context, AppDb::class.java, AppDb.DATABASE_NAME)
      .addMigrations(*migrations.toTypedArray())
      .build()
  }

  @Provides
  fun bindRoomDatabase(appDb: AppDb): RoomDatabase = appDb
}

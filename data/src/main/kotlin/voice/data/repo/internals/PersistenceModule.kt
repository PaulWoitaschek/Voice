package voice.data.repo.internals

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import voice.common.AppScope
import voice.data.repo.internals.dao.BookContentDao
import voice.data.repo.internals.dao.BookmarkDao
import voice.data.repo.internals.dao.ChapterDao
import voice.data.repo.internals.dao.LegacyBookDao
import javax.inject.Singleton

@Module
@ContributesTo(AppScope::class)
object PersistenceModule {

  @Provides
  fun chapterDao(appDb: AppDb): ChapterDao = appDb.chapterDao()

  @Provides
  fun bookContentDao(appDb: AppDb): BookContentDao = appDb.bookContentDao()

  @Provides
  fun bookmarkDao(appDb: AppDb): BookmarkDao = appDb.bookmarkDao()

  @Provides
  fun legacyBookDao(appDb: AppDb): LegacyBookDao = appDb.legacyBookDao()

  @Provides
  @Singleton
  fun appDb(
    context: Context,
    migrations: Set<@JvmSuppressWildcards Migration>,
  ): AppDb {
    return Room.databaseBuilder(context, AppDb::class.java, AppDb.DATABASE_NAME)
      .addMigrations(*migrations.toTypedArray())
      .build()
  }
}

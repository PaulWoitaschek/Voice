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
import voice.data.repo.internals.migrations.Migration23to24
import voice.data.repo.internals.migrations.Migration24to25
import voice.data.repo.internals.migrations.Migration25to26
import voice.data.repo.internals.migrations.Migration26to27
import voice.data.repo.internals.migrations.Migration27to28
import voice.data.repo.internals.migrations.Migration28to29
import voice.data.repo.internals.migrations.Migration29to30
import voice.data.repo.internals.migrations.Migration30to31
import voice.data.repo.internals.migrations.Migration31to32
import voice.data.repo.internals.migrations.Migration32to34
import voice.data.repo.internals.migrations.Migration34to35
import voice.data.repo.internals.migrations.Migration35to36
import voice.data.repo.internals.migrations.Migration36to37
import voice.data.repo.internals.migrations.Migration37to38
import voice.data.repo.internals.migrations.Migration38to39
import voice.data.repo.internals.migrations.Migration39to40
import voice.data.repo.internals.migrations.Migration40to41
import voice.data.repo.internals.migrations.Migration41to42
import voice.data.repo.internals.migrations.Migration42to43
import voice.data.repo.internals.migrations.Migration43to44
import voice.data.repo.internals.migrations.Migration44
import voice.data.repo.internals.migrations.Migration45
import voice.data.repo.internals.migrations.Migration46
import voice.data.repo.internals.migrations.Migration47
import voice.data.repo.internals.migrations.Migration48
import voice.data.repo.internals.migrations.Migration49
import voice.data.repo.internals.migrations.Migration50
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
    migrations: Array<Migration>
  ): AppDb {
    return Room.databaseBuilder(context, AppDb::class.java, AppDb.DATABASE_NAME)
      .addMigrations(*migrations)
      .build()
  }

  @Provides
  fun migrations(): Array<Migration> {
    return arrayOf(
      Migration23to24(),
      Migration24to25(),
      Migration25to26(),
      Migration26to27(),
      Migration27to28(),
      Migration28to29(),
      Migration29to30(),
      Migration30to31(),
      Migration31to32(),
      Migration32to34(),
      Migration34to35(),
      Migration35to36(),
      Migration36to37(),
      Migration37to38(),
      Migration38to39(),
      Migration39to40(),
      Migration40to41(),
      Migration41to42(),
      Migration42to43(),
      Migration43to44(),
      Migration44(),
      Migration45(),
      Migration46(),
      Migration47(),
      Migration48(),
      Migration49(),
      Migration50()
    )
  }
}

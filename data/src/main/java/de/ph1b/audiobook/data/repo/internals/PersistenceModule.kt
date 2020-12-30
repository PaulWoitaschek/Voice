package de.ph1b.audiobook.data.repo.internals

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.data.repo.internals.migrations.Migration23to24
import de.ph1b.audiobook.data.repo.internals.migrations.Migration24to25
import de.ph1b.audiobook.data.repo.internals.migrations.Migration25to26
import de.ph1b.audiobook.data.repo.internals.migrations.Migration26to27
import de.ph1b.audiobook.data.repo.internals.migrations.Migration27to28
import de.ph1b.audiobook.data.repo.internals.migrations.Migration28to29
import de.ph1b.audiobook.data.repo.internals.migrations.Migration29to30
import de.ph1b.audiobook.data.repo.internals.migrations.Migration30to31
import de.ph1b.audiobook.data.repo.internals.migrations.Migration31to32
import de.ph1b.audiobook.data.repo.internals.migrations.Migration32to34
import de.ph1b.audiobook.data.repo.internals.migrations.Migration34to35
import de.ph1b.audiobook.data.repo.internals.migrations.Migration35to36
import de.ph1b.audiobook.data.repo.internals.migrations.Migration36to37
import de.ph1b.audiobook.data.repo.internals.migrations.Migration37to38
import de.ph1b.audiobook.data.repo.internals.migrations.Migration38to39
import de.ph1b.audiobook.data.repo.internals.migrations.Migration39to40
import de.ph1b.audiobook.data.repo.internals.migrations.Migration40to41
import de.ph1b.audiobook.data.repo.internals.migrations.Migration41to42
import de.ph1b.audiobook.data.repo.internals.migrations.Migration42to43
import de.ph1b.audiobook.data.repo.internals.migrations.Migration43to44
import de.ph1b.audiobook.data.repo.internals.migrations.Migration44
import de.ph1b.audiobook.data.repo.internals.migrations.Migration45
import de.ph1b.audiobook.data.repo.internals.migrations.Migration46
import de.ph1b.audiobook.data.repo.internals.migrations.Migration47
import de.ph1b.audiobook.data.repo.internals.migrations.Migration48
import de.ph1b.audiobook.data.repo.internals.migrations.Migration49
import de.ph1b.audiobook.data.repo.internals.migrations.Migration50
import javax.inject.Singleton

@Module
@ContributesTo(AppScope::class)
object PersistenceModule {

  @Provides
  fun bookmarkDao(appDb: AppDb): BookmarkDao = appDb.bookmarkDao()

  @Provides
  fun chapterDao(appDb: AppDb): ChapterDao = appDb.chapterDao()

  @Provides
  fun metaDataDao(appDb: AppDb): BookMetaDataDao = appDb.bookMetadataDao()

  @Provides
  fun bookSettingsDao(appDb: AppDb): BookSettingsDao = appDb.bookSettingsDao()

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
  fun migrations(context: Context): Array<Migration> {
    return arrayOf(
      Migration23to24(),
      Migration24to25(context),
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

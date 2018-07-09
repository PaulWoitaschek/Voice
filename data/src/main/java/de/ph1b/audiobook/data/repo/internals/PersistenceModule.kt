package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import android.content.Context
import dagger.Module
import dagger.Provides
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
import de.ph1b.audiobook.data.repo.internals.migrations.Migration44to45
import javax.inject.Singleton

@Module
class PersistenceModule {

  @Provides
  fun bookmarkDao(appDb: AppDb) = appDb.bookmarkDao()

  @Provides
  fun chapterDao(appDb: AppDb) = appDb.chapterDao()

  @Provides
  fun metaDataDao(appDb: AppDb) = appDb.bookMetadataDao()

  @Provides
  fun bookSettingsDao(appDb: AppDb) = appDb.bookSettingsDao()

  @Provides
  fun roomDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDb> {
    return Room.databaseBuilder(context, AppDb::class.java, AppDb.DATABASE_NAME)
      .allowMainThreadQueries()
  }

  @Provides
  @Singleton
  fun appDb(
    builder: RoomDatabase.Builder<AppDb>,
    migrations: Array<Migration>
  ): AppDb {
    return builder
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
      Migration44to45()
    )
  }
}

package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PersistenceModule {

  @Provides
  fun bookmarkDao(appDb: AppDb) = appDb.bookmarkDao()

  @Provides
  @Singleton
  fun appDb(context: Context): AppDb {
    return Room.databaseBuilder(context, AppDb::class.java, "appDb")
      .build()
  }
}

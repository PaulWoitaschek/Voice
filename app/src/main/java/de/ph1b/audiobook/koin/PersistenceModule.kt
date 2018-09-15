package de.ph1b.audiobook.koin

import androidx.room.Room
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.data.repo.internals.AppDb
import de.ph1b.audiobook.data.repo.internals.BookStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val PersistenceModule = module {
  single { BookRepository(get()) }
  single { BookStorage(get(), get(), get(), get()) }
  single {
    Room.databaseBuilder(androidContext(), AppDb::class.java, AppDb.DATABASE_NAME)
      .allowMainThreadQueries()
      .build()
  }
  single { get<AppDb>().chapterDao() }
  single { get<AppDb>().bookMetadataDao() }
  single { get<AppDb>().bookSettingsDao() }
  single { get<AppDb>().bookmarkDao() }
  single { BookmarkRepo(get()) }
}

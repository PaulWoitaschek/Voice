package de.ph1b.audiobook.injection

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import de.paulwoitaschek.flowpref.Pref
import de.paulwoitaschek.flowpref.android.AndroidPreferences
import de.paulwoitaschek.flowpref.android.boolean
import de.paulwoitaschek.flowpref.android.enum
import de.paulwoitaschek.flowpref.android.int
import de.paulwoitaschek.flowpref.android.stringSet
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.common.pref.AudiobookFolders
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.features.bookOverview.GridMode
import de.ph1b.audiobook.misc.UUIDAdapter
import de.ph1b.audiobook.serialization.SerializableDataStoreFactory
import de.ph1b.audiobook.serialization.UriSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import java.util.UUID
import javax.inject.Named
import javax.inject.Singleton

@Module
@ContributesTo(AppScope::class)
object PrefsModule {

  @Provides
  @Singleton
  fun provideSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_preferences", Context.MODE_PRIVATE)
  }

  @Provides
  @Singleton
  fun prefs(sharedPreferences: SharedPreferences): AndroidPreferences {
    return AndroidPreferences(sharedPreferences)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.DARK_THEME)
  fun darkThemePref(prefs: AndroidPreferences): Pref<Boolean> {
    return prefs.boolean(PrefKeys.DARK_THEME, false)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.RESUME_ON_REPLUG)
  fun provideResumeOnReplugPreference(prefs: AndroidPreferences): Pref<Boolean> {
    return prefs.boolean(PrefKeys.RESUME_ON_REPLUG, true)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.AUTO_REWIND_AMOUNT)
  fun provideAutoRewindAmountPreference(prefs: AndroidPreferences): Pref<Int> {
    return prefs.int(PrefKeys.AUTO_REWIND_AMOUNT, 2)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.SEEK_TIME)
  fun provideSeekTimePreference(prefs: AndroidPreferences): Pref<Int> {
    return prefs.int(PrefKeys.SEEK_TIME, 20)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.SLEEP_TIME)
  fun provideSleepTimePreference(prefs: AndroidPreferences): Pref<Int> {
    return prefs.int(PrefKeys.SLEEP_TIME, 20)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.SINGLE_BOOK_FOLDERS)
  fun provideSingleBookFoldersPreference(prefs: AndroidPreferences): Pref<Set<String>> {
    return prefs.stringSet(PrefKeys.SINGLE_BOOK_FOLDERS, emptySet())
  }

  @Provides
  @Singleton
  @Named(PrefKeys.COLLECTION_BOOK_FOLDERS)
  fun provideCollectionFoldersPreference(prefs: AndroidPreferences): Pref<Set<String>> {
    return prefs.stringSet(PrefKeys.COLLECTION_BOOK_FOLDERS, emptySet())
  }

  @Provides
  @Singleton
  @Named(PrefKeys.CURRENT_BOOK)
  fun provideCurrentBookIdPreference(prefs: AndroidPreferences): Pref<UUID> {
    return prefs.create(PrefKeys.CURRENT_BOOK, UUID.randomUUID(), UUIDAdapter)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.GRID_MODE)
  fun gridViewPref(prefs: AndroidPreferences): Pref<GridMode> {
    return prefs.enum(PrefKeys.GRID_MODE, GridMode.FOLLOW_DEVICE)
  }

  @Provides
  @Singleton
  @AudiobookFolders
  fun audiobookFolders(factory: SerializableDataStoreFactory): DataStore<List<Uri>> {
    return factory.create(
      serializer = ListSerializer(UriSerializer),
      fileName = "audiobookFolders",
      defaultValue = emptyList()
    )
  }

  @Provides
  @Singleton
  @CurrentBook
  fun currentBook(factory: SerializableDataStoreFactory): DataStore<Uri?> {
    return factory.create(
      serializer = UriSerializer.nullable,
      fileName = "currentBook",
      defaultValue = null
    )
  }
}

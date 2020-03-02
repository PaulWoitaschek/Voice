package de.ph1b.audiobook.injection

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.features.bookOverview.GridMode
import de.ph1b.audiobook.persistence.pref.ReactivePrefs
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import java.util.UUID
import javax.inject.Named
import javax.inject.Singleton

@Module
object PrefsModule {

  @Provides
  @JvmStatic
  fun provideSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_preferences", Context.MODE_PRIVATE)
  }

  @Provides
  @JvmStatic
  @Singleton
  fun prefs(sharedPreferences: SharedPreferences): ReactivePrefs {
    return ReactivePrefs(sharedPreferences)
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.DARK_THEME)
  fun darkThemePref(prefs: ReactivePrefs): Pref<Boolean> {
    return prefs.boolean(PrefKeys.DARK_THEME, false)
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.RESUME_ON_REPLUG)
  fun provideResumeOnReplugPreference(prefs: ReactivePrefs): Pref<Boolean> {
    return prefs.boolean(PrefKeys.RESUME_ON_REPLUG, true)
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.BOOKMARK_ON_SLEEP)
  fun provideBookmarkOnSleepTimerPreference(prefs: ReactivePrefs): Pref<Boolean> {
    return prefs.boolean(PrefKeys.BOOKMARK_ON_SLEEP, false)
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.SHAKE_TO_RESET)
  fun provideShakeToResetPreference(prefs: ReactivePrefs): Pref<Boolean> {
    return prefs.boolean(PrefKeys.SHAKE_TO_RESET, false)
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.AUTO_REWIND_AMOUNT)
  fun provideAutoRewindAmountPreference(prefs: ReactivePrefs): Pref<Int> {
    return prefs.int(PrefKeys.AUTO_REWIND_AMOUNT, 2)
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.SEEK_TIME)
  fun provideSeekTimePreference(prefs: ReactivePrefs): Pref<Int> {
    return prefs.int(PrefKeys.SEEK_TIME, 20)
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.SLEEP_TIME)
  fun provideSleepTimePreference(prefs: ReactivePrefs): Pref<Int> {
    return prefs.int(PrefKeys.SLEEP_TIME, 20)
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.SINGLE_BOOK_FOLDERS)
  fun provideSingleBookFoldersPreference(prefs: ReactivePrefs): Pref<Set<String>> {
    return prefs.stringSet(PrefKeys.SINGLE_BOOK_FOLDERS, emptySet())
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.COLLECTION_BOOK_FOLDERS)
  fun provideCollectionFoldersPreference(prefs: ReactivePrefs): Pref<Set<String>> {
    return prefs.stringSet(PrefKeys.COLLECTION_BOOK_FOLDERS, emptySet())
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.CURRENT_BOOK)
  fun provideCurrentBookIdPreference(prefs: ReactivePrefs): Pref<UUID> {
    return prefs.uuid(PrefKeys.CURRENT_BOOK, UUID.randomUUID())
  }

  @Provides
  @JvmStatic
  @Singleton
  @Named(PrefKeys.GRID_MODE)
  fun gridViewPref(prefs: ReactivePrefs): Pref<GridMode> {
    return prefs.enum(PrefKeys.GRID_MODE, GridMode.FOLLOW_DEVICE)
  }
}

package de.ph1b.audiobook.injection

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences.RxSharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.persistence.pref.PersistentPref
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.ThemeUtil
import javax.inject.Named
import javax.inject.Singleton


@Module
class PrefsModule {

  @Provides
  fun provideSharedPreferences(context: Context): SharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(context)

  @Provides
  @Singleton
  fun provideRxSharedPreferences(sharedPreferences: SharedPreferences): RxSharedPreferences =
      RxSharedPreferences.create(sharedPreferences)

  @Provides
  @Reusable
  @Named(PrefKeys.THEME)
  fun provideThemePreference(prefs: RxSharedPreferences): Pref<ThemeUtil.Theme> {
    val pref = prefs.getEnum(PrefKeys.THEME, ThemeUtil.Theme.DAY_NIGHT, ThemeUtil.Theme::class.java)
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.RESUME_ON_REPLUG)
  fun provideResumeOnReplugPreference(prefs: RxSharedPreferences): Pref<Boolean> {
    val pref = prefs.getBoolean(PrefKeys.RESUME_ON_REPLUG, true)
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.BOOKMARK_ON_SLEEP)
  fun provideBookmarkOnSleepTimerPreference(prefs: RxSharedPreferences): Pref<Boolean> {
    val pref = prefs.getBoolean(PrefKeys.BOOKMARK_ON_SLEEP, false)
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.SHAKE_TO_RESET)
  fun provideShakeToResetPreference(prefs: RxSharedPreferences): Pref<Boolean> {
    val pref = prefs.getBoolean(PrefKeys.SHAKE_TO_RESET, false)
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.AUTO_REWIND_AMOUNT)
  fun provideAutoRewindAmountPreference(prefs: RxSharedPreferences): Pref<Int> {
    val pref = prefs.getInteger(PrefKeys.AUTO_REWIND_AMOUNT, 2)
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.SEEK_TIME)
  fun provideSeekTimePreference(prefs: RxSharedPreferences): Pref<Int> {
    val pref = prefs.getInteger(PrefKeys.SEEK_TIME, 20)
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.SLEEP_TIME)
  fun provideSleepTimePreference(prefs: RxSharedPreferences): Pref<Int> {
    val pref = prefs.getInteger(PrefKeys.SLEEP_TIME, 20)
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.SINGLE_BOOK_FOLDERS)
  fun provideSingleBookFoldersPreference(prefs: RxSharedPreferences): Pref<Set<String>> {
    val pref = prefs.getStringSet(PrefKeys.SINGLE_BOOK_FOLDERS, emptySet())
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.COLLECTION_BOOK_FOLDERS)
  fun provideCollectionFoldersPreference(prefs: RxSharedPreferences): Pref<Set<String>> {
    val pref = prefs.getStringSet(PrefKeys.COLLECTION_BOOK_FOLDERS, emptySet())
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.CURRENT_BOOK)
  fun provideCurrentBookIdPreference(prefs: RxSharedPreferences): Pref<Long> {
    val pref = prefs.getLong(PrefKeys.CURRENT_BOOK, Book.ID_UNKNOWN)
    return PersistentPref(pref)
  }

  @Provides
  @Reusable
  @Named(PrefKeys.RESUME_AFTER_CALL)
  fun provideResumeAfterCallPreference(prefs: RxSharedPreferences): Pref<Boolean> {
    val pref = prefs.getBoolean(PrefKeys.RESUME_AFTER_CALL, false)
    return PersistentPref(pref)
  }
}

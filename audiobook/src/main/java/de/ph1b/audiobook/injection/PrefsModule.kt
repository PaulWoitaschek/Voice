package de.ph1b.audiobook.injection

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences.Preference
import com.f2prateek.rx.preferences.RxSharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.book_overview.BookShelfController.DisplayMode
import de.ph1b.audiobook.uitools.ThemeUtil

/**
 * Module for preferences
 *
 * @author Paul Woitaschek
 */
@Module class PrefsModule {

  @Provides fun provideSharedPreferences(context: Context): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(context)
  }

  @Provides @Reusable fun provideRxSharedPreferences(sharedPreferences: SharedPreferences): RxSharedPreferences {
    return RxSharedPreferences.create(sharedPreferences)
  }

  @Provides @Reusable fun provideThemePreference(context: Context, prefs: RxSharedPreferences): Preference<ThemeUtil.Theme> {
    val key = context.getString(R.string.pref_key_theme)
    return prefs.getEnum(key, ThemeUtil.Theme.DAY_NIGHT, ThemeUtil.Theme::class.java)
  }

  @Provides @Reusable fun provideDisplayModePreference(prefs: RxSharedPreferences): Preference<DisplayMode> {
    val key = "displayMode"
    return prefs.getEnum(key, DisplayMode.GRID, DisplayMode::class.java)
  }

  @Provides @Reusable @ResumeOnReplug fun provideResumeOnReplugPreference(context: Context, prefs: RxSharedPreferences): Preference<Boolean> {
    val key = context.getString(R.string.pref_key_resume_on_replug)
    return prefs.getBoolean(key, true)
  }

  @Provides @Reusable @BookmarkOnSleepTimer fun provideBookmarkOnSleepTimerPreference(context: Context, prefs: RxSharedPreferences): Preference<Boolean> {
    val key = context.getString(R.string.pref_key_bookmark_on_sleep)
    return prefs.getBoolean(key, false)
  }

  @Provides @Reusable @ShakeToReset fun provideShakeToResetPreference(context: Context, prefs: RxSharedPreferences): Preference<Boolean> {
    val key = context.getString(R.string.pref_key_shake_to_reset_sleep_timer)
    return prefs.getBoolean(key, false)
  }

  @Provides @Reusable @PauseOnTempFocusLoss fun providePauseOnTempFocusLossPreference(context: Context, prefs: RxSharedPreferences): Preference<Boolean> {
    val key = context.getString(R.string.pref_key_pause_on_can_duck)
    return prefs.getBoolean(key, false)
  }

  @Provides @Reusable @AutoRewindAmount fun provideAutoRewindAmountPreference(context: Context, prefs: RxSharedPreferences): Preference<Int> {
    val key = context.getString(R.string.pref_key_auto_rewind)
    return prefs.getInteger(key, 2)
  }

  @Provides @Reusable @SeekTime fun provideSeekTimePreference(context: Context, prefs: RxSharedPreferences): Preference<Int> {
    val key = context.getString(R.string.pref_key_seek_time)
    return prefs.getInteger(key, 20)
  }

  @Provides @Reusable @SleepTime fun provideSleepTimePreference(context: Context, prefs: RxSharedPreferences): Preference<Int> {
    val key = context.getString(R.string.pref_key_sleep_time)
    return prefs.getInteger(key, 20)
  }

  @Provides @Reusable @SingleBookFolders fun provideSingleBookFoldersPreference(prefs: RxSharedPreferences): Preference<Set<String>> {
    val key = "singleBookFolders"
    return prefs.getStringSet(key, emptySet())
  }

  @Provides @Reusable @CollectionFolders fun provideCollectionFoldersPreference(prefs: RxSharedPreferences): Preference<Set<String>> {
    val key = "folders"
    return prefs.getStringSet(key, emptySet())
  }

  @Provides @Reusable @CurrentBookId fun provideCurrentBookIdPreference(prefs: RxSharedPreferences): Preference<Long> {
    val key = "currentBook"
    return prefs.getLong(key, Book.ID_UNKNOWN)
  }

    @Provides @Reusable @BroadCastTrackInformation fun broadCastTrackInformationPreference(context: Context, prefs: RxSharedPreferences): Preference<Boolean> {
      val key = context.getString(R.string.pref_key_broadcast_track_information)
      return prefs.getBoolean(key, true)
    }
}
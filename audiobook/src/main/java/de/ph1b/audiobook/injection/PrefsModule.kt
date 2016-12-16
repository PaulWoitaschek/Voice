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

  @Provides fun provideSharedPreferences(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

  @Provides @Reusable fun provideRxSharedPreferences(sharedPreferences: SharedPreferences) = RxSharedPreferences.create(sharedPreferences)

  @Provides @Reusable fun provideThemePreference(context: Context, prefs: RxSharedPreferences): Preference<ThemeUtil.Theme> {
    return prefs.getEnum(context.getString(R.string.pref_key_theme), ThemeUtil.Theme.DAY_NIGHT, ThemeUtil.Theme::class.java)
  }

  @Provides @Reusable fun provideDisplayModePreference(prefs: RxSharedPreferences) = prefs.getEnum("displayMode", DisplayMode.GRID, DisplayMode::class.java)

  @Provides @Reusable @ResumeOnReplug fun provideResumeOnReplugPreference(context: Context, prefs: RxSharedPreferences) =
    prefs.getBoolean(context.getString(R.string.pref_key_resume_on_replug), true)

  @Provides @Reusable @BookmarkOnSleepTimer fun provideBookmarkOnSleepTimerPreference(context: Context, prefs: RxSharedPreferences) =
    prefs.getBoolean(context.getString(R.string.pref_key_bookmark_on_sleep), false)

  @Provides @Reusable @ShakeToReset fun provideShakeToResetPreference(context: Context, prefs: RxSharedPreferences) =
    prefs.getBoolean(context.getString(R.string.pref_key_shake_to_reset_sleep_timer), false)

  @Provides @Reusable @PauseOnTempFocusLoss fun providePauseOnTempFocusLossPreference(context: Context, prefs: RxSharedPreferences) =
    prefs.getBoolean(context.getString(R.string.pref_key_pause_on_can_duck), false)

  @Provides @Reusable @Analytics fun provideAnalyticPreference(context: Context, prefs: RxSharedPreferences) =
    prefs.getBoolean(context.getString(R.string.pref_key_analytics), true)

  @Provides @Reusable @AutoRewindAmount fun provideAutoRewindAmountPreference(context: Context, prefs: RxSharedPreferences) =
    prefs.getInteger(context.getString(R.string.pref_key_auto_rewind), 2)

  @Provides @Reusable @SeekTime fun provideSeekTimePreference(context: Context, prefs: RxSharedPreferences) =
    prefs.getInteger(context.getString(R.string.pref_key_seek_time), 20)

  @Provides @Reusable @SleepTime fun provideSleepTimePreference(context: Context, prefs: RxSharedPreferences) =
    prefs.getInteger(context.getString(R.string.pref_key_sleep_time), 20)

  @Provides @Reusable @SingleBookFolders fun provideSingleBookFoldersPreference(prefs: RxSharedPreferences) = prefs.getStringSet("singleBookFolders", emptySet())

  @Provides @Reusable @CollectionFolders fun provideCollectionFoldersPreference(prefs: RxSharedPreferences): Preference<Set<String>> = prefs.getStringSet("folders", emptySet())

  @Provides @Reusable @CurrentBookId fun provideCurrentBookIdPreference(prefs: RxSharedPreferences) = prefs.getLong("currentBook", Book.ID_UNKNOWN)
}
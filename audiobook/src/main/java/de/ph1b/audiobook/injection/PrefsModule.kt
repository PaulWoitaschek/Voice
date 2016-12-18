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
import de.ph1b.audiobook.features.bookOverview.BookShelfController.DisplayMode
import de.ph1b.audiobook.uitools.ThemeUtil

/**
 * Module for preferences
 *
 * @author Paul Woitaschek
 */
@Module class PrefsModule {

  @Provides fun provideSharedPreferences(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)!!
  @Provides @Reusable fun provideRxSharedPreferences(sharedPreferences: SharedPreferences) = RxSharedPreferences.create(sharedPreferences)
  @Provides @Reusable fun provideThemePreference(prefs: RxSharedPreferences) = prefs.getEnum("THEME2_KEY", ThemeUtil.Theme.DAY_NIGHT, ThemeUtil.Theme::class.java)
  @Provides @Reusable fun provideDisplayModePreference(prefs: RxSharedPreferences) = prefs.getEnum("displayMode", DisplayMode.GRID, DisplayMode::class.java)
  @Provides @Reusable @ResumeOnReplug fun provideResumeOnReplugPreference(prefs: RxSharedPreferences) = prefs.getBoolean("RESUME_ON_REPLUG", true)
  @Provides @Reusable @BookmarkOnSleepTimer fun provideBookmarkOnSleepTimerPreference(prefs: RxSharedPreferences) = prefs.getBoolean("BOOKMARK_ON_SLEEP", false)
  @Provides @Reusable @ShakeToReset fun provideShakeToResetPreference(prefs: RxSharedPreferences) = prefs.getBoolean("SHAKE_TO_RESET_SLEEP_TIMER", false)
  @Provides @Reusable @PauseOnTempFocusLoss fun providePauseOnTempFocusLossPreference(prefs: RxSharedPreferences) = prefs.getBoolean("PAUSE_ON_CAN_DUCK", false)
  @Provides @Reusable @Analytics fun provideAnalyticPreference(prefs: RxSharedPreferences) = prefs.getBoolean("analytics", true)
  @Provides @Reusable @AutoRewindAmount fun provideAutoRewindAmountPreference(prefs: RxSharedPreferences) = prefs.getInteger("AUTO_REWIND", 2)
  @Provides @Reusable @SeekTime fun provideSeekTimePreference(prefs: RxSharedPreferences) = prefs.getInteger("SEEK_TIME", 20)
  @Provides @Reusable @SleepTime fun provideSleepTimePreference(prefs: RxSharedPreferences) = prefs.getInteger("SLEEP_TIME", 20)
  @Provides @Reusable @SingleBookFolders fun provideSingleBookFoldersPreference(prefs: RxSharedPreferences) = prefs.getStringSet("singleBookFolders", emptySet())
  @Provides @Reusable @CollectionFolders fun provideCollectionFoldersPreference(prefs: RxSharedPreferences): Preference<Set<String>> = prefs.getStringSet("folders", emptySet())
  @Provides @Reusable @CurrentBookId fun provideCurrentBookIdPreference(prefs: RxSharedPreferences) = prefs.getLong("currentBook", Book.ID_UNKNOWN)
}
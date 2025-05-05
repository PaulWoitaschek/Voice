package voice.app.injection

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import voice.app.BuildConfig
import voice.bookOverview.BookMigrationExplanationQualifier
import voice.bookOverview.BookMigrationExplanationShown
import voice.common.AppScope
import voice.common.BookId
import voice.common.grid.GridMode
import voice.common.pref.AuthorAudiobookFolders
import voice.common.pref.CurrentBook
import voice.common.pref.OnboardingCompleted
import voice.common.pref.PrefKeys
import voice.common.pref.RootAudiobookFolders
import voice.common.pref.SingleFileAudiobookFolders
import voice.common.pref.SingleFolderAudiobookFolders
import voice.common.serialization.UriSerializer
import voice.datastore.VoiceDataStoreFactory
import voice.pref.AndroidPreferences
import voice.pref.Pref
import voice.pref.boolean
import voice.pref.enum
import voice.pref.int
import voice.pref.stringSet
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
  @Named(PrefKeys.GRID_MODE)
  fun gridViewPref(prefs: AndroidPreferences): Pref<GridMode> {
    return prefs.enum(PrefKeys.GRID_MODE, GridMode.FOLLOW_DEVICE)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.AUTO_SLEEP_TIMER_ENABLED)
  fun provideAutoSleepTimerEnabledPreference(prefs: AndroidPreferences): Pref<Boolean> {
    return prefs.boolean(PrefKeys.AUTO_SLEEP_TIMER_ENABLED, false)
  }

  @Provides
  @Singleton
  @Named(PrefKeys.AUTO_SLEEP_TIMER_START_TIME)
  fun provideAutoSleepTimerStartTimePreference(prefs: AndroidPreferences): Pref<Int> {
    return prefs.int(PrefKeys.AUTO_SLEEP_TIMER_START_TIME, 22) // Default: 22:00
  }

  @Provides
  @Singleton
  @Named(PrefKeys.AUTO_SLEEP_TIMER_END_TIME)
  fun provideAutoSleepTimerEndTimePreference(prefs: AndroidPreferences): Pref<Int> {
    return prefs.int(PrefKeys.AUTO_SLEEP_TIMER_END_TIME, 6) // Default: 06:00
  }

  @Provides
  @Singleton
  @Named(PrefKeys.AUTO_SLEEP_TIMER_DURATION)
  fun provideAutoSleepTimerDurationPreference(prefs: AndroidPreferences): Pref<Int> {
    return prefs.int(PrefKeys.AUTO_SLEEP_TIMER_DURATION, 30) // Default: 30 minutes
  }

  @Provides
  @Singleton
  @OnboardingCompleted
  fun onboardingCompleted(factory: VoiceDataStoreFactory): DataStore<Boolean> {
    return factory.boolean("onboardingCompleted", defaultValue = false)
  }

  @Provides
  @Singleton
  @RootAudiobookFolders
  fun audiobookFolders(factory: VoiceDataStoreFactory): DataStore<List<Uri>> {
    return factory.createUriList("audiobookFolders")
  }

  @Provides
  @Singleton
  @SingleFolderAudiobookFolders
  fun singleFolderAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<List<Uri>> {
    return factory.createUriList("SingleFolderAudiobookFolders")
  }

  @Provides
  @Singleton
  @SingleFileAudiobookFolders
  fun singleFileAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<List<Uri>> {
    return factory.createUriList("SingleFileAudiobookFolders")
  }

  @Provides
  @Singleton
  @AuthorAudiobookFolders
  fun authorAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<List<Uri>> {
    return factory.createUriList("AuthorAudiobookFolders")
  }

  @Provides
  @Singleton
  @CurrentBook
  fun currentBook(factory: VoiceDataStoreFactory): DataStore<BookId?> {
    return factory.create(
      serializer = BookId.serializer().nullable,
      fileName = "currentBook",
      defaultValue = null,
    )
  }

  @Provides
  @Singleton
  @BookMigrationExplanationQualifier
  fun bookMigrationExplanationShown(factory: VoiceDataStoreFactory): BookMigrationExplanationShown {
    return factory.create(Boolean.serializer(), false, "bookMigrationExplanationShown2")
  }
}

private fun VoiceDataStoreFactory.createUriList(name: String): DataStore<List<Uri>> = create(
  serializer = ListSerializer(UriSerializer),
  fileName = name,
  defaultValue = emptyList(),
)

package voice.app.injection

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import voice.app.BuildConfig
import voice.bookOverview.BookMigrationExplanationQualifier
import voice.bookOverview.BookMigrationExplanationShown
import voice.common.AppScope
import voice.common.BookId
import voice.common.grid.GridMode
import voice.common.pref.AuthorAudiobookFoldersStore
import voice.common.pref.AutoRewindAmountStore
import voice.common.pref.CurrentBookStore
import voice.common.pref.DarkThemeStore
import voice.common.pref.GridModeStore
import voice.common.pref.OnboardingCompletedStore
import voice.common.pref.RootAudiobookFoldersStore
import voice.common.pref.SeekTimeStore
import voice.common.pref.SingleFileAudiobookFoldersStore
import voice.common.pref.SingleFolderAudiobookFoldersStore
import voice.common.pref.SleepTimeStore
import voice.common.serialization.UriSerializer
import voice.datastore.VoiceDataStoreFactory
import voice.pref.AndroidPreferences
import voice.pref.boolean
import voice.pref.enum
import voice.pref.int
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
  @DarkThemeStore
  fun darkThemePref(prefs: AndroidPreferences): DataStore<Boolean> {
    return prefs.boolean("darkTheme", false)
  }

  @Provides
  @Singleton
  @AutoRewindAmountStore
  fun provideAutoRewindAmountPreference(prefs: AndroidPreferences): DataStore<Int> {
    return prefs.int("AUTO_REWIND", 2)
  }

  @Provides
  @Singleton
  @SeekTimeStore
  fun provideSeekTimePreference(prefs: AndroidPreferences): DataStore<Int> {
    return prefs.int("SEEK_TIME", 20)
  }

  @Provides
  @Singleton
  @SleepTimeStore
  fun provideSleepTimePreference(prefs: AndroidPreferences): DataStore<Int> {
    return prefs.int("SLEEP_TIME", 20)
  }

  @Provides
  @Singleton
  @GridModeStore
  fun gridViewPref(prefs: AndroidPreferences): DataStore<GridMode> {
    return prefs.enum("gridView", GridMode.FOLLOW_DEVICE)
  }

  @Provides
  @Singleton
  @OnboardingCompletedStore
  fun onboardingCompleted(factory: VoiceDataStoreFactory): DataStore<Boolean> {
    return factory.boolean("onboardingCompleted", defaultValue = false)
  }

  @Provides
  @Singleton
  @RootAudiobookFoldersStore
  fun audiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("audiobookFolders")
  }

  @Provides
  @Singleton
  @SingleFolderAudiobookFoldersStore
  fun singleFolderAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("SingleFolderAudiobookFolders")
  }

  @Provides
  @Singleton
  @SingleFileAudiobookFoldersStore
  fun singleFileAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("SingleFileAudiobookFolders")
  }

  @Provides
  @Singleton
  @AuthorAudiobookFoldersStore
  fun authorAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("AuthorAudiobookFolders")
  }

  @Provides
  @Singleton
  @CurrentBookStore
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

private fun VoiceDataStoreFactory.createUriSet(name: String): DataStore<Set<Uri>> = create(
  serializer = SetSerializer(UriSerializer),
  fileName = name,
  defaultValue = emptySet(),
)

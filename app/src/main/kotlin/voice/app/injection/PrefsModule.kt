package voice.app.injection

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import voice.app.BuildConfig
import voice.bookOverview.BookMigrationExplanationQualifier
import voice.bookOverview.BookMigrationExplanationShown
import voice.common.BookId
import voice.common.grid.GridMode
import voice.common.pref.AuthorAudiobookFoldersStore
import voice.common.pref.AutoRewindAmountStore
import voice.common.pref.CurrentBookStore
import voice.common.pref.DarkThemeStore
import voice.common.pref.FadeOutStore
import voice.common.pref.GridModeStore
import voice.common.pref.OnboardingCompletedStore
import voice.common.pref.RootAudiobookFoldersStore
import voice.common.pref.SeekTimeStore
import voice.common.pref.SingleFileAudiobookFoldersStore
import voice.common.pref.SingleFolderAudiobookFoldersStore
import voice.common.pref.SleepTimerPreferenceStore
import voice.common.serialization.UriSerializer
import voice.common.sleepTimer.SleepTimerPreference
import voice.datastore.VoiceDataStoreFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@BindingContainer
@ContributesTo(AppScope::class)
object PrefsModule {

  @Provides
  @SingleIn(AppScope::class)
  fun sharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences("${BuildConfig.APPLICATION_ID}_preferences", Context.MODE_PRIVATE)
  }

  @Provides
  @SingleIn(AppScope::class)
  @DarkThemeStore
  fun darkTheme(
    factory: VoiceDataStoreFactory,
    sharedPreferences: SharedPreferences,
  ): DataStore<Boolean> {
    return factory.boolean(
      fileName = "darkTheme",
      defaultValue = false,
      migrations = listOf(
        booleanPrefsDataMigration(sharedPreferences, "darkTheme"),
      ),
    )
  }

  @Provides
  @SingleIn(AppScope::class)
  @AutoRewindAmountStore
  fun autoRewindAmount(
    factory: VoiceDataStoreFactory,
    sharedPreferences: SharedPreferences,
  ): DataStore<Int> {
    return factory.int(
      fileName = "autoRewind",
      defaultValue = 2,
      migrations = listOf(intPrefsDataMigration(sharedPreferences, "AUTO_REWIND")),
    )
  }

  @Provides
  @SingleIn(AppScope::class)
  @FadeOutStore
  fun fadeOut(factory: VoiceDataStoreFactory): DataStore<Duration> {
    return factory.create(
      fileName = "fadeOut",
      defaultValue = 10.seconds,
      serializer = Duration.serializer(),
    )
  }

  @Provides
  @SingleIn(AppScope::class)
  @SeekTimeStore
  fun seekTime(
    factory: VoiceDataStoreFactory,
    sharedPreferences: SharedPreferences,
  ): DataStore<Int> {
    return factory.int(
      fileName = "seekTime",
      defaultValue = 20,
      migrations = listOf(intPrefsDataMigration(sharedPreferences, "SEEK_TIME")),
    )
  }

  @Provides
  @SingleIn(AppScope::class)
  @SleepTimerPreferenceStore
  fun sleepTimerPreference(factory: VoiceDataStoreFactory): DataStore<SleepTimerPreference> {
    return factory.create(
      serializer = SleepTimerPreference.serializer(),
      fileName = "sleepTime3",
      defaultValue = SleepTimerPreference.Default,
    )
  }

  @Provides
  @SingleIn(AppScope::class)
  @GridModeStore
  fun gridMode(
    factory: VoiceDataStoreFactory,
    sharedPreferences: SharedPreferences,
  ): DataStore<GridMode> {
    return factory.create(
      GridMode.serializer(),
      GridMode.FOLLOW_DEVICE,
      "gridMode",
      migrations = listOf(
        PrefsDataMigration(
          sharedPreferences,
          key = "gridView",
          getFromSharedPreferences = {
            when (sharedPreferences.getString("gridView", null)) {
              "LIST" -> GridMode.LIST
              "GRID" -> GridMode.GRID
              else -> GridMode.FOLLOW_DEVICE
            }
          },
        ),
      ),
    )
  }

  @Provides
  @SingleIn(AppScope::class)
  @OnboardingCompletedStore
  fun onboardingCompleted(factory: VoiceDataStoreFactory): DataStore<Boolean> {
    return factory.boolean("onboardingCompleted", defaultValue = false)
  }

  @Provides
  @SingleIn(AppScope::class)
  @RootAudiobookFoldersStore
  fun audiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("audiobookFolders")
  }

  @Provides
  @SingleIn(AppScope::class)
  @SingleFolderAudiobookFoldersStore
  fun singleFolderAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("SingleFolderAudiobookFolders")
  }

  @Provides
  @SingleIn(AppScope::class)
  @SingleFileAudiobookFoldersStore
  fun singleFileAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("SingleFileAudiobookFolders")
  }

  @Provides
  @SingleIn(AppScope::class)
  @AuthorAudiobookFoldersStore
  fun authorAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("AuthorAudiobookFolders")
  }

  @Provides
  @SingleIn(AppScope::class)
  @CurrentBookStore
  fun currentBook(factory: VoiceDataStoreFactory): DataStore<BookId?> {
    return factory.create(
      serializer = BookId.serializer().nullable,
      fileName = "currentBook",
      defaultValue = null,
    )
  }

  @Provides
  @SingleIn(AppScope::class)
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

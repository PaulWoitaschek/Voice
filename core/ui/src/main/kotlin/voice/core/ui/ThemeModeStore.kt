package voice.core.ui

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory
import java.io.File

@SingleIn(AppScope::class)
class ThemeModeStore(store: DataStore<ThemeMode>) : DataStore<ThemeMode> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
    application: Application,
    sharedPreferences: SharedPreferences,
  ) : this(
    factory.create(
      serializer = ThemeMode.serializer(),
      fileName = "themeMode",
      defaultValue = ThemeMode.FollowSystem,
      migrations = listOf(
        LegacyDarkThemeMigration(application, sharedPreferences),
      ),
    ),
  )
}

private class LegacyDarkThemeMigration(
  application: Application,
  private val sharedPreferences: SharedPreferences,
) : DataMigration<ThemeMode> {

  private val oldDataStoreFile = File(application.applicationContext.filesDir, "datastore/darkTheme")

  override suspend fun cleanUp() {
    oldDataStoreFile.delete()
    sharedPreferences.edit {
      remove("darkTheme")
    }
  }

  override suspend fun migrate(currentData: ThemeMode): ThemeMode {
    val legacyValue = when {
      oldDataStoreFile.exists() -> oldDataStoreFile.readText().trim().toBooleanStrictOrNull()
      sharedPreferences.contains("darkTheme") -> sharedPreferences.getBoolean("darkTheme", false)
      else -> null
    }
    return when (legacyValue) {
      true -> ThemeMode.Dark
      false -> ThemeMode.Light
      null -> currentData
    }
  }

  override suspend fun shouldMigrate(currentData: ThemeMode): Boolean {
    return oldDataStoreFile.exists() || sharedPreferences.contains("darkTheme")
  }
}

package voice.core.data.store

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataMigration

internal class PrefsDataMigration<T>(
  private val sharedPreferences: SharedPreferences,
  private val key: String,
  private val getFromSharedPreferences: () -> T,
) : DataMigration<T> {
  override suspend fun cleanUp() {
    sharedPreferences.edit {
      remove(key)
    }
  }

  override suspend fun migrate(currentData: T): T {
    return getFromSharedPreferences()
  }

  override suspend fun shouldMigrate(currentData: T): Boolean {
    return sharedPreferences.contains(key)
  }
}

internal fun booleanPrefsDataMigration(
  sharedPreferences: SharedPreferences,
  key: String,
): DataMigration<Boolean> {
  return PrefsDataMigration(
    sharedPreferences = sharedPreferences,
    key = key,
    getFromSharedPreferences = {
      sharedPreferences.getBoolean(key, false)
    },
  )
}

internal fun intPrefsDataMigration(
  sharedPreferences: SharedPreferences,
  key: String,
): DataMigration<Int> {
  return PrefsDataMigration(
    sharedPreferences = sharedPreferences,
    key = key,
    getFromSharedPreferences = {
      sharedPreferences.getInt(key, 0)
    },
  )
}

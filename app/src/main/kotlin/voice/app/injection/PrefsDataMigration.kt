package voice.app.injection

import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.datastore.core.DataMigration

class PrefsDataMigration<T>(
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

fun stringSetToUriSetMigration(
  sharedPreferences: SharedPreferences,
  key: String,
): DataMigration<Set<Uri>> {
  return PrefsDataMigration(
    sharedPreferences = sharedPreferences,
    key = key,
    getFromSharedPreferences = {
      sharedPreferences.getStringSet(key, emptySet())
        ?.map(String::toUri)?.toSet() ?: emptySet()
    },
  )
}

fun booleanPrefsDataMigration(
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

fun intPrefsDataMigration(
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

package voice.core.datastore

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataMigration
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

fun secondsPrefsDataMigration(
  sharedPreferences: SharedPreferences,
  key: String,
): DataMigration<Duration> {
  return PrefsDataMigration(
    sharedPreferences = sharedPreferences,
    key = key,
    getFromSharedPreferences = {
      sharedPreferences.getInt(key, 0).seconds
    },
  )
}

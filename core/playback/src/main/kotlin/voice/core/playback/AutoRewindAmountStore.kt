package voice.core.playback

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory
import voice.core.datastore.intPrefsDataMigration

@SingleIn(AppScope::class)
class AutoRewindAmountStore(store: DataStore<Int>) : DataStore<Int> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
    sharedPreferences: SharedPreferences,
  ) : this(
    factory.int(
      fileName = "autoRewind",
      defaultValue = 2,
      migrations = listOf(intPrefsDataMigration(sharedPreferences, "AUTO_REWIND")),
    ),
  )
}

package voice.core.playback

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import voice.core.datastore.DurationInSecondsSerializer
import voice.core.datastore.VoiceDataStoreFactory
import voice.core.datastore.secondsPrefsDataMigration

@SingleIn(AppScope::class)
class AutoRewindAmountStore(store: DataStore<Duration>) : DataStore<Duration> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
    sharedPreferences: SharedPreferences,
  ) : this(
    factory.create(
      serializer = DurationInSecondsSerializer,
      fileName = "autoRewind",
      defaultValue = 2.seconds,
      migrations = listOf(secondsPrefsDataMigration(sharedPreferences, "AUTO_REWIND")),
    ),
  )
}

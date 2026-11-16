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
class SeekTimeStore(store: DataStore<Duration>) : DataStore<Duration> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
    sharedPreferences: SharedPreferences,
  ) : this(
    factory.create(
      serializer = DurationInSecondsSerializer,
      fileName = "seekTime",
      defaultValue = 20.seconds,
      migrations = listOf(secondsPrefsDataMigration(sharedPreferences, "SEEK_TIME")),
    ),
  )
}

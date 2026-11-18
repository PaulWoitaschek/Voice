package voice.core.playback.playstate

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import voice.core.datastore.VoiceDataStoreFactory

// A value left from an earlier process means that the process was killed during playback.
@SingleIn(AppScope::class)
class PlayingSaveIntervalStore(store: DataStore<Long?>) : DataStore<Long?> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
  ) : this(
    factory.create(
      serializer = Long.serializer().nullable,
      defaultValue = null,
      fileName = "playingSaveInterval",
    ),
  )
}

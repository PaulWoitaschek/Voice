package voice.core.playback

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.serializer
import voice.core.datastore.VoiceDataStoreFactory

/**
 * The speed the listener last played at or picked, which a book that was never played starts at.
 */
@SingleIn(AppScope::class)
class LastPlaybackSpeedStore(store: DataStore<Float>) : DataStore<Float> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.create(
      serializer = Float.serializer(),
      defaultValue = 1F,
      fileName = "lastPlaybackSpeed",
    ),
  )
}

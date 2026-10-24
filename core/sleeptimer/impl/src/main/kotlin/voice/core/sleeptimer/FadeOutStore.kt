package voice.core.sleeptimer

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.serializer
import voice.core.datastore.VoiceDataStoreFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SingleIn(AppScope::class)
internal class FadeOutStore(store: DataStore<Duration>) : DataStore<Duration> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.create(
      fileName = "fadeOut",
      defaultValue = 10.seconds,
      serializer = Duration.serializer(),
    ),
  )
}

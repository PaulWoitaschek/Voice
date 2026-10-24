package voice.core.data.supporter

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
public class SupporterStatusStore(store: DataStore<SupporterStatus>) : DataStore<SupporterStatus> by store {

  @Inject
  public constructor(factory: VoiceDataStoreFactory) : this(
    factory.create(
      serializer = SupporterStatus.serializer(),
      defaultValue = SupporterStatus(),
      fileName = "supporterStatus",
    ),
  )
}

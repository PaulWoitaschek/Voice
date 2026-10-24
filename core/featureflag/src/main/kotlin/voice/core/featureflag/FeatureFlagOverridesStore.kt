package voice.core.featureflag

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
class FeatureFlagOverridesStore(store: DataStore<Map<String, FeatureFlagOverride>>) : DataStore<Map<String, FeatureFlagOverride>> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.create(
      serializer = MapSerializer(String.serializer(), FeatureFlagOverride.serializer()),
      defaultValue = emptyMap(),
      fileName = "featureFlagOverrides",
    ),
  )
}

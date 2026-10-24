package voice.features.bookOverview.survey

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import voice.core.datastore.VoiceDataStoreFactory

/**
 * Each survey URL the listener opened or dismissed, with the day it happened on as an epoch day.
 */
@SingleIn(AppScope::class)
class HandledSurveysStore(store: DataStore<Map<String, Long>>) : DataStore<Map<String, Long>> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.create(
      serializer = MapSerializer(String.serializer(), Long.serializer()),
      defaultValue = emptyMap(),
      fileName = "handledSurveys",
    ),
  )
}

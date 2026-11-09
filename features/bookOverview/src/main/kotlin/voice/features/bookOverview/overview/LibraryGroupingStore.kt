package voice.features.bookOverview.overview

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.Serializable
import voice.core.datastore.VoiceDataStoreFactory

@Serializable
enum class LibraryGrouping {
  Series,
  Author,
}

@SingleIn(AppScope::class)
class LibraryGroupingStore(store: DataStore<LibraryGrouping>) :
  DataStore<LibraryGrouping> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
  ) : this(
    factory.create(LibraryGrouping.serializer(), LibraryGrouping.Series, "libraryGrouping"),
  )
}

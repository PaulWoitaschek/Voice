package voice.features.bookmark

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory

/**
 * Whether the bookmarks screen had the history open last, so it opens on that tab again. It starts
 * on the history.
 */
@SingleIn(AppScope::class)
class HistoryTabOpenStore(store: DataStore<Boolean>) : DataStore<Boolean> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.boolean("historyTabOpen", defaultValue = true),
  )
}

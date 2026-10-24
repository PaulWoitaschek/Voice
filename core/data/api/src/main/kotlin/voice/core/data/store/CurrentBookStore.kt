package voice.core.data.store

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.nullable
import voice.core.data.BookId
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
public class CurrentBookStore(store: DataStore<BookId?>) : DataStore<BookId?> by store {

  @Inject
  public constructor(factory: VoiceDataStoreFactory) : this(
    factory.create(
      serializer = BookId.serializer().nullable,
      fileName = "currentBook",
      defaultValue = null,
    ),
  )
}

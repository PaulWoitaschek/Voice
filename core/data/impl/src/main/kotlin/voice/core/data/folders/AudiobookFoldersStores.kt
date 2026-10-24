package voice.core.data.folders

import android.net.Uri
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.SetSerializer
import voice.core.common.serialization.UriSerializer
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
internal class RootAudiobookFoldersStore(store: DataStore<Set<Uri>>) : DataStore<Set<Uri>> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(factory.createUriSet("audiobookFolders"))
}

@SingleIn(AppScope::class)
internal class SingleFolderAudiobookFoldersStore(store: DataStore<Set<Uri>>) : DataStore<Set<Uri>> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(factory.createUriSet("SingleFolderAudiobookFolders"))
}

@SingleIn(AppScope::class)
internal class SingleFileAudiobookFoldersStore(store: DataStore<Set<Uri>>) : DataStore<Set<Uri>> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(factory.createUriSet("SingleFileAudiobookFolders"))
}

@SingleIn(AppScope::class)
internal class AuthorAudiobookFoldersStore(store: DataStore<Set<Uri>>) : DataStore<Set<Uri>> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(factory.createUriSet("AuthorAudiobookFolders"))
}

private fun VoiceDataStoreFactory.createUriSet(name: String): DataStore<Set<Uri>> = create(
  serializer = SetSerializer(UriSerializer),
  fileName = name,
  defaultValue = emptySet(),
)

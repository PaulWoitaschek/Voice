package voice.core.data.folders

import android.net.Uri
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.SetSerializer
import voice.core.common.serialization.UriSerializer
import voice.core.data.store.VoiceDataStoreFactory

@BindingContainer
@ContributesTo(AppScope::class)
public object AudiobookFoldersGraph {

  @Provides
  @SingleIn(AppScope::class)
  @RootAudiobookFoldersStore
  private fun audiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("audiobookFolders")
  }

  @Provides
  @SingleIn(AppScope::class)
  @SingleFolderAudiobookFoldersStore
  private fun singleFolderAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("SingleFolderAudiobookFolders")
  }

  @Provides
  @SingleIn(AppScope::class)
  @SingleFileAudiobookFoldersStore
  private fun singleFileAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("SingleFileAudiobookFolders")
  }

  @Provides
  @SingleIn(AppScope::class)
  @AuthorAudiobookFoldersStore
  private fun authorAudiobookFolders(factory: VoiceDataStoreFactory): DataStore<Set<Uri>> {
    return factory.createUriSet("AuthorAudiobookFolders")
  }
}

private fun VoiceDataStoreFactory.createUriSet(name: String): DataStore<Set<Uri>> = create(
  serializer = SetSerializer(UriSerializer),
  fileName = name,
  defaultValue = emptySet(),
)

package voice.features.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import voice.core.audiobookshelf.ServerLibrary
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.data.BookId

internal class FakeServerLibrary(
  override val serverName: MutableStateFlow<String?> = MutableStateFlow(null),
  override val unavailableBooks: MutableStateFlow<Set<BookId>> = MutableStateFlow(emptySet()),
  override val downloadStates: MutableStateFlow<Map<BookId, BookDownloadState>> = MutableStateFlow(emptyMap()),
) : ServerLibrary {

  override val syncing: Flow<Boolean> = MutableStateFlow(false)

  var syncs = 0
    private set

  override suspend fun isConnected(): Boolean = serverName.value != null

  override fun sync() {
    syncs++
  }
}

package voice.features.audiobookshelf.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import voice.core.audiobookshelf.Audiobookshelf
import voice.core.audiobookshelf.AudiobookshelfConnection
import voice.core.audiobookshelf.download.AudiobookshelfDownloads
import voice.core.audiobookshelf.sync.AudiobookshelfSync
import voice.core.audiobookshelf.sync.ServerReachability
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin

@Inject
class AudiobookshelfSettingsViewModel(
  private val audiobookshelf: Audiobookshelf,
  private val sync: AudiobookshelfSync,
  private val downloads: AudiobookshelfDownloads,
  private val navigator: Navigator,
  dispatcherProvider: DispatcherProvider,
) {

  private val scope = MainScope(dispatcherProvider)
  private var libraries by mutableStateOf<List<LibraryViewState>?>(null)
  private var librariesUnavailable by mutableStateOf(false)
  private var confirmSignOut by mutableStateOf(false)
  private var signedOut = false

  @Composable
  internal fun viewState(): AudiobookshelfSettingsViewState? {
    val connection: AudiobookshelfConnection? by remember { audiobookshelf.connection }.collectAsState(initial = null)
    val reachability by sync.reachability.collectAsState()
    val syncing by sync.syncing.collectAsState()
    val usedBytes by remember { downloads.usedBytes }.collectAsState(initial = 0L)
    val downloadOverMobileData by remember { downloads.downloadOverMobileData }.collectAsState(initial = false)
    // the libraries come in once the server answers
    LaunchedEffect(reachability) {
      if (libraries == null) loadLibraries()
    }
    val current = connection ?: return null
    return AudiobookshelfSettingsViewState(
      serverName = current.serverName,
      username = current.username,
      status = when {
        current.needsLogin -> ServerStatus.NeedsLogin
        syncing -> ServerStatus.Syncing
        reachability == ServerReachability.Unreachable -> ServerStatus.Offline
        else -> ServerStatus.Connected
      },
      libraries = libraries?.map { it.copy(selected = it.id in current.libraryIds) },
      librariesUnavailable = librariesUnavailable,
      usedBytes = usedBytes,
      downloadOverMobileData = downloadOverMobileData,
      confirmSignOut = confirmSignOut,
    )
  }

  private suspend fun loadLibraries() {
    val preview = audiobookshelf.libraries()
    librariesUnavailable = preview == null
    if (preview == null) return
    libraries = preview.libraries.map { library ->
      LibraryViewState(id = library.id, name = library.name, bookCount = library.bookCount, selected = false)
    }
  }

  internal fun onLibraryToggle(
    id: String,
    selectedIds: List<String>,
  ) {
    val updated = if (id in selectedIds) selectedIds - id else selectedIds + id
    // without a library there would be nothing left to sync
    if (updated.isEmpty()) return
    scope.launch {
      audiobookshelf.setLibraries(updated)
    }
  }

  internal fun onDownloadOverMobileDataChange(enabled: Boolean) {
    scope.launch {
      downloads.setDownloadOverMobileData(enabled)
    }
  }

  internal fun onRemoveDownloads() {
    scope.launch {
      downloads.removeAll()
    }
  }

  internal fun onSignInAgain() {
    navigator.goTo(Destination.AudiobookshelfLogin(Origin.Default, renew = true))
  }

  internal fun onSyncNow() {
    sync.sync(force = true)
  }

  internal fun onSignOut() {
    confirmSignOut = true
  }

  internal fun onDismissSignOut() {
    confirmSignOut = false
  }

  internal fun onConfirmSignOut() {
    if (signedOut) return
    signedOut = true
    confirmSignOut = false
    scope.launch {
      audiobookshelf.signOut()
      navigator.goBack()
    }
  }

  internal fun onBack() {
    navigator.goBack()
  }
}

internal data class AudiobookshelfSettingsViewState(
  val serverName: String,
  val username: String,
  val status: ServerStatus,
  val libraries: List<LibraryViewState>?,
  /** The server didn't answer, so its libraries can't be picked right now. */
  val librariesUnavailable: Boolean = false,
  val usedBytes: Long,
  val downloadOverMobileData: Boolean,
  val confirmSignOut: Boolean,
) {
  val selectedLibraryIds: List<String> get() = libraries.orEmpty().filter { it.selected }.map { it.id }
}

internal data class LibraryViewState(
  val id: String,
  val name: String,
  val bookCount: Int,
  val selected: Boolean,
)

internal enum class ServerStatus {
  Connected,
  Syncing,
  Offline,
  NeedsLogin,
}

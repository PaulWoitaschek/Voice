package voice.features.bookOverview.bottomSheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import voice.core.audiobookshelf.ServerLibrary
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.core.featureflag.ExperimentalPlaybackPersistenceQualifier
import voice.core.featureflag.FeatureFlag
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.features.bookOverview.di.BookActionsScope
import voice.features.bookOverview.overview.category
import voice.features.bookOverview.overview.toItemViewState
import voice.navigation.Destination
import voice.navigation.Navigator

@SingleIn(BookActionsScope::class)
@Inject
class BottomSheetViewModel(
  private val bookId: BookId,
  private val viewModels: Set<@JvmSuppressWildcards BottomSheetItemViewModel>,
  private val repo: BookRepository,
  private val serverLibrary: ServerLibrary,
  private val navigator: Navigator,
  private val playerController: PlayerController,
  @ExperimentalPlaybackPersistenceQualifier
  private val experimentalPlaybackPersistenceFeatureFlag: FeatureFlag<Boolean>,
) {

  private val scope = MainScope()

  /** Null until the book is loaded. Follows the book from then on, e.g. its progress while it plays. */
  @Composable
  internal fun state(): EditBookBottomSheetState? {
    val book = remember { book() }.collectAsState(initial = null).value
    val items = produceState<List<BottomSheetItem>?>(initialValue = null) {
      value = viewModels.flatMap { it.items(bookId) }
        .toSet()
        .sorted()
    }.value
    val server = remember { serverLibrary.serverName.map(::ServerName) }.collectAsState(initial = null).value
    // the download goes on while the sheet is open
    val download = if (bookId.isRemote) {
      remember { serverLibrary.downloadStates.map { it[bookId] ?: BookDownloadState.NotDownloaded }.distinctUntilChanged() }
        .collectAsState(initial = null).value ?: return null
    } else {
      null
    }
    val askForNotifications = produceState<Boolean?>(initialValue = null) {
      value = bookId.isRemote && serverLibrary.shouldAskForNotifications()
    }.value
    if (book == null || items == null || server == null || askForNotifications == null) return null
    return EditBookBottomSheetState(
      book = book.toItemViewState(),
      category = book.category,
      items = (items + download?.items().orEmpty()).toSet().sorted(),
      source = source(server.name, download),
      download = download,
      bookSize = book.chapters.takeIf { chapters -> chapters.all { it.fileSize > 0 } }?.sumOf { it.fileSize } ?: 0L,
      askForNotifications = askForNotifications,
    )
  }

  // with books only on the device there is nothing to tell apart
  private fun source(
    serverName: String?,
    download: BookDownloadState?,
  ): BookSource? {
    if (serverName == null) return null
    if (!bookId.isRemote) return BookSource.Device
    return BookSource.Server(
      name = serverName,
      download = if (download is BookDownloadState.Downloaded) BookSource.Download.Done else BookSource.Download.None,
    )
  }

  private fun book(): Flow<Book> {
    val book = repo.flow(bookId)
      .onEach { book ->
        // with no book there is no menu to show, and an invisible one would swallow the next back press
        if (book == null) onClose()
      }
      .filterNotNull()
    // with the experimental persistence, the stored position of the playing book lags behind
    return if (experimentalPlaybackPersistenceFeatureFlag.get()) {
      book.combine(playerController.livePlaybackStateFlow(bookId)) { book, livePlaybackState ->
        livePlaybackState?.let(book::overlay) ?: book
      }
    } else {
      book
    }
  }

  /**
   * Returns once the item is handled, e.g. when the sheet it opens has its state. The work itself runs in this
   * scope, so it completes even if the caller goes away.
   */
  internal suspend fun onItemClick(item: BottomSheetItem) {
    scope.launch {
      when (item) {
        BottomSheetItem.Download, BottomSheetItem.RetryDownload -> serverLibrary.download(bookId)
        BottomSheetItem.StopDownload, BottomSheetItem.RemoveDownload -> serverLibrary.removeDownload(bookId)
        else -> viewModels.forEach {
          it.onItemClick(bookId, item)
        }
      }
    }.join()
  }

  internal fun onAskedForNotifications() {
    scope.launch {
      serverLibrary.onAskedForNotifications()
    }
  }

  internal fun onClose() {
    navigator.remove(Destination.BookActions(bookId))
  }
}

private class ServerName(val name: String?)

private fun BookDownloadState.items(): List<BottomSheetItem> = when (this) {
  BookDownloadState.NotDownloaded -> listOf(BottomSheetItem.Download)
  is BookDownloadState.Downloading -> listOf(BottomSheetItem.StopDownload)
  BookDownloadState.Failed -> listOf(BottomSheetItem.RetryDownload, BottomSheetItem.RemoveDownload)
  is BookDownloadState.Downloaded -> listOf(BottomSheetItem.RemoveDownload)
}

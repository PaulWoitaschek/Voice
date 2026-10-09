package voice.features.bookOverview.bottomSheet

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.audiobookshelf.ServerLibrary
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.di.BookOverviewScope
import voice.features.bookOverview.overview.category
import voice.features.bookOverview.overview.toItemViewState

@SingleIn(BookOverviewScope::class)
@Inject
class BottomSheetViewModel(
  private val viewModels: Set<@JvmSuppressWildcards BottomSheetItemViewModel>,
  private val repo: BookRepository,
  private val serverLibrary: ServerLibrary,
) {

  private val scope = MainScope()
  private var loadJob: Job? = null

  internal val state: State<EditBookBottomSheetState>
    field = mutableStateOf(EditBookBottomSheetState.Empty)

  var bookId: BookId? = null
    private set

  internal fun bookSelected(bookId: BookId) {
    this.bookId = bookId
    // the previous book must not flash up while this one loads
    state.value = EditBookBottomSheetState.Empty
    loadJob?.cancel()
    loadJob = scope.launch {
      val book = repo.get(bookId)
      val items = viewModels.flatMap { it.items(bookId) }.toSet()
      val loaded = EditBookBottomSheetState(
        book = book?.toItemViewState(),
        category = book?.category,
        items = items.sorted(),
      )
      if (!bookId.isRemote) {
        // with books only on the device there is nothing to tell apart
        val serverConnected = serverLibrary.serverName.first() != null
        state.value = loaded.copy(source = if (serverConnected) BookSource.Device else null)
        return@launch
      }
      val askForNotifications = serverLibrary.shouldAskForNotifications()
      val bookSize = book?.chapters?.takeIf { chapters -> chapters.all { it.fileSize > 0 } }?.sumOf { it.fileSize } ?: 0L
      // the download goes on while the sheet is open
      combine(
        serverLibrary.serverName,
        serverLibrary.downloadStates.map { it[bookId] ?: BookDownloadState.NotDownloaded }.distinctUntilChanged(),
      ) { serverName, download ->
        loaded.copy(
          items = (items + download.items()).sorted(),
          bookSize = bookSize,
          source = serverName?.let {
            BookSource.Server(
              name = it,
              download = if (download is BookDownloadState.Downloaded) BookSource.Download.Done else BookSource.Download.None,
            )
          },
          download = download,
          askForNotifications = askForNotifications,
        )
      }.collect { state.value = it }
    }
  }

  internal fun onItemClick(item: BottomSheetItem) {
    val bookId = bookId ?: return
    scope.launch {
      when (item) {
        BottomSheetItem.Download, BottomSheetItem.RetryDownload -> serverLibrary.download(bookId)
        BottomSheetItem.StopDownload, BottomSheetItem.RemoveDownload -> serverLibrary.removeDownload(bookId)
        else -> viewModels.forEach {
          it.onItemClick(bookId, item)
        }
      }
    }
  }

  internal fun onAskedForNotifications() {
    scope.launch {
      serverLibrary.onAskedForNotifications()
    }
  }
}

private fun BookDownloadState.items(): List<BottomSheetItem> = when (this) {
  BookDownloadState.NotDownloaded -> listOf(BottomSheetItem.Download)
  is BookDownloadState.Downloading -> listOf(BottomSheetItem.StopDownload)
  BookDownloadState.Failed -> listOf(BottomSheetItem.RetryDownload, BottomSheetItem.RemoveDownload)
  is BookDownloadState.Downloaded -> listOf(BottomSheetItem.RemoveDownload)
}

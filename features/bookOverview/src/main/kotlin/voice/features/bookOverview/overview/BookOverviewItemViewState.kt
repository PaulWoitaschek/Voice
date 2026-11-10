package voice.features.bookOverview.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import voice.core.audiobookshelf.ServerDownloads
import voice.core.audiobookshelf.ServerLibrary
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.audiobookshelf.download.WaitingFor
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.logging.api.Logger
import voice.core.ui.formatTime

@Immutable
data class BookOverviewItemViewState(
  val name: String,
  val author: String?,
  val cover: String?,
  val progress: Float,
  val id: BookId,
  val remainingTime: String,
  val series: String? = null,
  /** A server book that isn't downloaded while the server can't be reached. */
  val unavailable: Boolean = false,
  val download: DownloadBadge? = null,
  val hidden: Boolean = false,
)

@Immutable
sealed interface DownloadBadge {
  data class Running(
    val progress: Float,
    val waitingFor: WaitingFor?,
  ) : DownloadBadge

  data object Failed : DownloadBadge
}

internal fun BookDownloadState?.badge(): DownloadBadge? =
  when (this) {
    is BookDownloadState.Downloading -> DownloadBadge.Running(progress, waitingFor)
    BookDownloadState.Failed -> DownloadBadge.Failed
    is BookDownloadState.Downloaded,
    BookDownloadState.NotDownloaded,
    null -> null
  }

@Immutable
internal data class ServerBookStates(
  val unavailable: Set<BookId>,
  val downloads: Map<BookId, BookDownloadState>?,
)

@Composable
internal fun rememberServerBookStates(
  serverLibrary: ServerLibrary,
  serverDownloads: ServerDownloads,
): ServerBookStates {
  val unavailable = remember {
    serverLibrary.unavailableBooks
  }
    .collectAsState(initial = emptySet())
    .value
  val downloads = remember { serverDownloads.states }.collectAsState(initial = null).value
  return ServerBookStates(unavailable, downloads)
}

internal fun Book.toItemViewState(serverStates: ServerBookStates) =
  toItemViewState(
    unavailable = id in serverStates.unavailable,
    download = serverStates.downloads?.get(id).badge(),
  )

internal fun Book.toItemViewState(
  unavailable: Boolean = false,
  download: DownloadBadge? = null,
) =
  BookOverviewItemViewState(
    name = name,
    author = author,
    cover = coverUrl,
    id = id,
    progress = progress(),
    remainingTime = formatTime(duration - position),
    series = seriesLine,
    unavailable = unavailable,
    download = download,
    hidden = custom.hidden,
  )

internal fun Book.progress(): Float {
  val globalPosition = position
  val totalDuration = duration
  if (totalDuration == 0L) return 0F
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    Logger.w("Negative progress: position=$globalPosition, duration=$totalDuration")
  }
  return progress.coerceIn(0F, 1F)
}

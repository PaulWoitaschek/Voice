package voice.features.bookOverview.overview

import androidx.compose.runtime.Immutable
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
  /** A server book that isn't downloaded while the server can't be reached. */
  val unavailable: Boolean = false,
  /** What the cover of a server book tells about its download, while it isn't done. */
  val download: DownloadBadge? = null,
)

@Immutable
sealed interface DownloadBadge {
  data class Running(
    val progress: Float,
    val waitingFor: WaitingFor?,
  ) : DownloadBadge

  data object Failed : DownloadBadge
}

internal fun BookDownloadState?.badge(): DownloadBadge? = when (this) {
  is BookDownloadState.Downloading -> DownloadBadge.Running(progress, waitingFor)
  BookDownloadState.Failed -> DownloadBadge.Failed
  is BookDownloadState.Downloaded, BookDownloadState.NotDownloaded, null -> null
}

internal fun Book.toItemViewState(
  unavailable: Boolean = false,
  download: DownloadBadge? = null,
) = BookOverviewItemViewState(
  name = content.name,
  author = content.author,
  cover = content.coverUrl,
  id = id,
  progress = progress(),
  remainingTime = formatTime(duration - position),
  unavailable = unavailable,
  download = download,
)

private fun Book.progress(): Float {
  val globalPosition = position
  val totalDuration = duration
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    Logger.w("Couldn't determine progress for book=$this")
  }
  return progress.coerceIn(0F, 1F)
}

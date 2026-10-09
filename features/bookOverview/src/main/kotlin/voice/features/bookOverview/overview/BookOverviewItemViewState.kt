package voice.features.bookOverview.overview

import androidx.compose.runtime.Immutable
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
  /** How far the download of a server book has come, while it downloads. */
  val downloadProgress: Float? = null,
)

internal fun Book.toItemViewState(
  unavailable: Boolean = false,
  downloadProgress: Float? = null,
) = BookOverviewItemViewState(
  name = content.name,
  author = content.author,
  cover = content.coverUrl,
  id = id,
  progress = progress(),
  remainingTime = formatTime(duration - position),
  unavailable = unavailable,
  downloadProgress = downloadProgress,
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

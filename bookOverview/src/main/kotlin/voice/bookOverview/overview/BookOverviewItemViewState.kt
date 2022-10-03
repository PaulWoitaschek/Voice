package voice.bookOverview.overview

import android.text.format.DateUtils
import androidx.compose.runtime.Immutable
import voice.common.BookId
import voice.common.compose.ImmutableFile
import voice.data.Book
import voice.logging.core.Logger

@Immutable
data class BookOverviewItemViewState(
  val name: String,
  val author: String?,
  val cover: ImmutableFile?,
  val progress: Float,
  val id: BookId,
  val remainingTime: String,
)

internal fun Book.toItemViewState() = BookOverviewItemViewState(
  name = content.name,
  author = content.author,
  cover = content.cover?.let(::ImmutableFile),
  id = id,
  progress = progress(),
  remainingTime = DateUtils.formatElapsedTime((duration - position) / 1000),
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

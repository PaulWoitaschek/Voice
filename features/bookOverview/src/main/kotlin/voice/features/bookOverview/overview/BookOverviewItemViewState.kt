package voice.features.bookOverview.overview

import android.text.format.DateUtils
import androidx.compose.runtime.Immutable
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.logging.core.Logger
import voice.core.ui.ImmutableFile

@Immutable
data class BookOverviewItemViewState(
  val name: String,
  val author: String?,
  val cover: ImmutableFile?,
  val progress: Float,
  val id: BookId,
  val remainingTime: String,
  val genre: String? = "",
  val narrator: String? = "",
  val series: String? = "",
  val part: String? = ""
)

internal fun Book.toItemViewState() = BookOverviewItemViewState(
  name = content.name,
  author = content.author,
  cover = content.cover?.let(::ImmutableFile),
  id = id,
  progress = progress(),
  remainingTime = DateUtils.formatElapsedTime((duration - position) / 1000),
  genre = content.genre,
  narrator = content.narrator,
  series = content.series,
  part = content.part
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

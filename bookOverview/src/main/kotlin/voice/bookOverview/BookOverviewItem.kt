package voice.bookOverview

import androidx.annotation.FloatRange
import voice.data.Book
import voice.logging.core.Logger
import java.io.File

sealed class BookOverviewItem

data class BookOverviewHeaderModel(
  val category: BookOverviewCategory,
) : BookOverviewItem()

data class BookOverviewViewState(
  val name: String,
  val author: String?,
  val transitionName: String,
  @FloatRange(from = 0.0, to = 1.0)
  val progress: Float,
  val remainingTimeInMs: Long,
  val isCurrentBook: Boolean,
  val useGridView: Boolean,
  val id: Book.Id,
  val cover: File?,
) : BookOverviewItem() {

  constructor(book: Book, amountOfColumns: Int, currentBookId: Book.Id?) : this(
    name = book.content.name,
    author = book.content.author,
    transitionName = book.transitionName,
    progress = book.progress(),
    remainingTimeInMs = book.remainingTimeInMs(),
    isCurrentBook = book.id == currentBookId,
    useGridView = amountOfColumns > 1,
    id = book.id,
    cover = book.content.cover,
  )

  fun areContentsTheSame(other: BookOverviewViewState): Boolean {
    return this == other
  }

  fun areItemsTheSame(other: BookOverviewViewState): Boolean {
    return id == other.id
  }
}

private fun Book.progress(): Float {
  val globalPosition = position
  val totalDuration = duration
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    Logger.w("Couldn't determine progress for book=$this")
  }
  return progress.coerceIn(0F, 1F)
}

private fun Book.remainingTimeInMs(): Long {
  return duration - position
}

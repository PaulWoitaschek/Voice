package de.ph1b.audiobook.features.bookOverview.list

import android.net.Uri
import androidx.annotation.FloatRange
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import timber.log.Timber
import java.io.File

sealed class BookOverviewItem

data class BookOverviewHeaderModel(
  val category: BookOverviewCategory,
  val hasMore: Boolean
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
  val id: Uri,
  val cover: File?,
) : BookOverviewItem() {

  constructor(book: BookContent2, amountOfColumns: Int, currentBookId: Uri?) : this(
    name = book.name,
    author = book.author,
    transitionName = book.uri.toString(),
    progress = book.progress(),
    remainingTimeInMs = book.remainingTimeInMs(),
    isCurrentBook = book.uri == currentBookId,
    useGridView = amountOfColumns > 1,
    id = book.uri,
    cover = book.cover,
  )

  fun areContentsTheSame(other: BookOverviewViewState): Boolean {
    return this == other
  }

  fun areItemsTheSame(other: BookOverviewViewState): Boolean {
    return id == other.id
  }
}

private fun BookContent2.progress(): Float {
  val globalPosition = position
  val totalDuration = duration
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    Timber.e("Couldn't determine progress for book=$this")
  }
  return progress.coerceIn(0F, 1F)
}

private fun BookContent2.remainingTimeInMs(): Long {
  return duration - position
}

package de.ph1b.audiobook.features.bookOverview.list

import androidx.annotation.FloatRange
import de.ph1b.audiobook.crashreporting.CrashReporter
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory

sealed class BookOverviewItem

data class BookOverviewHeaderModel(
  val category: BookOverviewCategory,
  val hasMore: Boolean
) : BookOverviewItem()

data class BookOverviewModel(
  val name: String,
  val author: String?,
  val transitionName: String,
  @FloatRange(from = 0.0, to = 1.0)
  val progress: Float,
  val book: Book,
  val remainingTimeInMs: Long,
  val isCurrentBook: Boolean,
  val useGridView: Boolean
) : BookOverviewItem() {

  constructor(book: Book, isCurrentBook: Boolean, useGridView: Boolean) : this(
    name = book.name,
    author = book.author,
    transitionName = book.coverTransitionName,
    book = book,
    progress = book.progress(),
    remainingTimeInMs = book.remainingTimeInMs(),
    isCurrentBook = isCurrentBook,
    useGridView = useGridView
  )

  fun areContentsTheSame(other: BookOverviewModel): Boolean {
    val oldBook = book
    val newBook = other.book
    return oldBook.id == newBook.id &&
        oldBook.content.position == newBook.content.position &&
        name == other.name &&
        isCurrentBook == other.isCurrentBook &&
        useGridView == other.useGridView
  }

  fun areItemsTheSame(other: BookOverviewModel): Boolean {
    return book.id == other.book.id
  }
}

private fun Book.progress(): Float {
  val globalPosition = content.position
  val totalDuration = content.duration
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    CrashReporter.logException(
      AssertionError(
        "Couldn't determine progress for book=$this. Progress is $progress, " +
            "globalPosition=$globalPosition, totalDuration=$totalDuration"
      )
    )
  }
  return progress.coerceIn(0F, 1F)
}

private fun Book.remainingTimeInMs(): Long {
  return content.duration - content.position
}

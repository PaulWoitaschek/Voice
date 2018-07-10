package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.data.Book

sealed class BookOverviewState {

  data class Content(
    val currentBook: Book?,
    val playing: Boolean,
    val currentBooks: List<Book>,
    val notStartedBooks: List<Book>,
    val completedBooks: List<Book>
  ) : BookOverviewState() {

    val books = currentBooks + notStartedBooks + completedBooks
  }

  object Loading : BookOverviewState()

  object NoFolderSet : BookOverviewState()
}

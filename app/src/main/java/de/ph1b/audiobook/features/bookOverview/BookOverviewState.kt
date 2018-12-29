package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.features.bookOverview.list.BookOverviewModel

sealed class BookOverviewState {

  data class Content(
    val playing: Boolean,
    val currentBooks: List<BookOverviewModel>,
    val notStartedBooks: List<BookOverviewModel>,
    val completedBooks: List<BookOverviewModel>,
    val currentBookPresent: Boolean
  ) : BookOverviewState()

  object Loading : BookOverviewState()

  object NoFolderSet : BookOverviewState()
}

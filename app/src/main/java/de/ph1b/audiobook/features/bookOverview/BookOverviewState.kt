package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.data.Book

sealed class BookOverviewState {

  data class Content(
    val books: List<Book>,
    val currentBook: Book?,
    val playing: Boolean
  ) : BookOverviewState()

  object Loading : BookOverviewState()

  object NoFolderSet : BookOverviewState()
}

package de.ph1b.audiobook.features.bookOverview

import de.ph1b.audiobook.features.bookOverview.list.BookOverviewModel
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory

sealed class BookOverviewState {

  data class Content(
    val playing: Boolean,
    val currentBookPresent: Boolean,
    val categoriesWithContents: Map<BookOverviewCategory, BookOverviewCategoryContent>,
    val useGrid: Boolean
  ) : BookOverviewState()

  object Loading : BookOverviewState()

  object NoFolderSet : BookOverviewState()
}

data class BookOverviewCategoryContent(
  val books: List<BookOverviewModel>,
  val hasMore: Boolean
)

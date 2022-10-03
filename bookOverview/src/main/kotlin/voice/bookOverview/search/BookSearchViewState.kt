package voice.bookOverview.search

import voice.bookOverview.overview.BookOverviewItemViewState
import voice.bookOverview.overview.BookOverviewLayoutMode

internal data class BookSearchViewState(
  val query: String,
  val books: List<BookOverviewItemViewState>,
  val layoutMode: BookOverviewLayoutMode,
)

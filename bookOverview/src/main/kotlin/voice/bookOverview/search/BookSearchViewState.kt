package voice.bookOverview.search

import voice.bookOverview.overview.BookOverviewItemViewState
import voice.bookOverview.overview.BookOverviewLayoutMode

internal sealed interface BookSearchViewState {
  val query: String

  data class SearchResults(
    val books: List<BookOverviewItemViewState>,
    val layoutMode: BookOverviewLayoutMode,
    override val query: String,
  ) : BookSearchViewState

  data class InactiveSearch(
    val suggestedAuthors: List<String>,
    val recentQueries: List<String>,
    override val query: String,
  ) : BookSearchViewState
}

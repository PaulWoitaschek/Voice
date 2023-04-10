package voice.bookOverview.search

import androidx.compose.runtime.Immutable
import voice.bookOverview.overview.BookOverviewItemViewState
import voice.bookOverview.overview.BookOverviewLayoutMode

@Immutable
sealed interface BookSearchViewState {
  val query: String

  data class SearchResults(
    val books: List<BookOverviewItemViewState>,
    val layoutMode: BookOverviewLayoutMode,
    override val query: String,
  ) : BookSearchViewState

  data class EmptySearch(
    val suggestedAuthors: List<String>,
    val recentQueries: List<String>,
    override val query: String,
  ) : BookSearchViewState
}
